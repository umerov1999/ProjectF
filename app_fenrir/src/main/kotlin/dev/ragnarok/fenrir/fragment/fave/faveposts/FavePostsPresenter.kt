package dev.ragnarok.fenrir.fragment.fave.faveposts

import android.os.Bundle
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.findInfoByPredicate
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.dummy
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain

class FavePostsPresenter(accountId: Long, savedInstanceState: Bundle?) :
    PlaceSupportPresenter<IFavePostsView>(accountId, savedInstanceState) {
    private val posts: MutableList<Post> = ArrayList()
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val wallInteractor: IWallsRepository = walls
    private val cacheCompositeDisposable = CompositeJob()
    private var requestNow = false
    private var actualInfoReceived = false
    private var nextOffset = 0
    private var endOfContent = false
    private var doLoadTabs = false
    private fun onPostUpdate(update: PostUpdate) {
        val likeUpdate = update.likeUpdate ?: return
        // likes only
        val info = findInfoByPredicate(
            posts
        ) {
            it.vkid == update.postId && it.ownerId == update.ownerId
        }
        if (info != null) {
            val post = info.second
            if (accountId == update.accountId) {
                post.setUserLikes(likeUpdate.isLiked)
            }
            post.setLikesCount(likeUpdate.count)
            view?.notifyItemChanged(
                info.first
            )
        }
    }

    private fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveRefreshingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        requestActual(0)
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            requestNow
        )
    }

    override fun onGuiCreated(viewHost: IFavePostsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(posts)
    }

    private fun requestActual(offset: Int) {
        setRequestNow(true)
        val newOffset = offset + COUNT
        appendJob(faveInteractor.getPosts(accountId, COUNT, offset)
            .fromIOToMain({ posts ->
                onActualDataReceived(
                    offset,
                    newOffset,
                    posts
                )
            }) { throwable -> onActualDataGetError(throwable) })
    }

    private fun onActualDataGetError(throwable: Throwable) {
        setRequestNow(false)
        showError(throwable)
    }

    private fun onActualDataReceived(offset: Int, newOffset: Int, data: List<Post>) {
        setRequestNow(false)
        nextOffset = newOffset
        endOfContent = data.isEmpty()
        actualInfoReceived = true
        if (offset == 0) {
            posts.clear()
            posts.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val sizeBefore = posts.size
            posts.addAll(data)
            view?.notifyDataAdded(
                sizeBefore,
                data.size
            )
        }
    }

    private fun loadCachedData() {
        cacheCompositeDisposable.add(faveInteractor.getCachedPosts(accountId)
            .fromIOToMain({ posts -> onCachedDataReceived(posts) }) { obj -> obj.printStackTrace() })
    }

    private fun onCachedDataReceived(posts: List<Post>) {
        this.posts.clear()
        this.posts.addAll(posts)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheCompositeDisposable.cancel()
        super.onDestroyed()
    }

    fun fireRefresh() {
        if (!requestNow) {
            requestActual(0)
        }
    }

    fun fireScrollToEnd() {
        if (posts.isNotEmpty() && actualInfoReceived && !requestNow && !endOfContent) {
            requestActual(nextOffset)
        }
    }

    fun fireLikeClick(post: Post) {
        if (Settings.get().main().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        appendJob(wallInteractor.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain(dummy()) { t -> onLikeError(t) })
    }

    fun firePostDelete(index: Int, post: Post) {
        appendJob(faveInteractor.removePost(accountId, post.ownerId, post.vkid)
            .fromIOToMain({
                posts.removeAt(index)
                view?.notifyDataSetChanged()
            }) { throwable -> onActualDataGetError(throwable) })
    }

    private fun onLikeError(t: Throwable) {
        showError(t)
    }

    companion object {
        private const val COUNT = 50
    }

    init {
        appendJob(wallInteractor.observeMinorChanges()
            .sharedFlowToMain { onPostUpdate(it) })
        loadCachedData()
    }
}