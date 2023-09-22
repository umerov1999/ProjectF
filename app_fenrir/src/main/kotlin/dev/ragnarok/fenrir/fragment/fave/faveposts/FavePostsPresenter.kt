package dev.ragnarok.fenrir.fragment.fave.faveposts

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.db.model.PostUpdate
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.findInfoByPredicate
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class FavePostsPresenter(accountId: Long, savedInstanceState: Bundle?) :
    PlaceSupportPresenter<IFavePostsView>(accountId, savedInstanceState) {
    private val posts: MutableList<Post>
    private val faveInteractor: IFaveInteractor
    private val wallInteractor: IWallsRepository
    private val cacheCompositeDisposable = CompositeDisposable()
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

    public override fun onGuiResumed() {
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
        appendDisposable(faveInteractor.getPosts(accountId, COUNT, offset)
            .fromIOToMain()
            .subscribe({ posts ->
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
            .fromIOToMain()
            .subscribe({ posts -> onCachedDataReceived(posts) }) { obj -> obj.printStackTrace() })
    }

    private fun onCachedDataReceived(posts: List<Post>) {
        this.posts.clear()
        this.posts.addAll(posts)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheCompositeDisposable.dispose()
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
        appendDisposable(wallInteractor.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain()
            .subscribe(ignore()) { t -> onLikeError(t) })
    }

    fun firePostDelete(index: Int, post: Post) {
        appendDisposable(faveInteractor.removePost(accountId, post.ownerId, post.vkid)
            .fromIOToMain()
            .subscribe({
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
        posts = ArrayList()
        faveInteractor = InteractorFactory.createFaveInteractor()
        wallInteractor = walls
        appendDisposable(wallInteractor.observeMinorChanges()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onPostUpdate(it) })
        loadCachedData()
    }
}