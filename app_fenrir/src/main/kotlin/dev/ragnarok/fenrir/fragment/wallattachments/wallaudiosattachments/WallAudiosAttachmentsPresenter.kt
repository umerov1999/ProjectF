package dev.ragnarok.fenrir.fragment.wallattachments.wallaudiosattachments

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.domain.IWallsRepository
import dev.ragnarok.fenrir.domain.Repository.walls
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.intValueIn
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import dev.ragnarok.fenrir.util.rxutils.RxUtils.ignore
import io.reactivex.rxjava3.disposables.CompositeDisposable

class WallAudiosAttachmentsPresenter(
    accountId: Int,
    private val owner_id: Int,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<IWallAudiosAttachmentsView>(accountId, savedInstanceState) {
    private val mAudios: ArrayList<Post> = ArrayList()
    private val fInteractor: IWallsRepository = walls
    private val actualDataDisposable = CompositeDisposable()
    private var loaded = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    override fun onGuiCreated(viewHost: IWallAudiosAttachmentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mAudios)
        resolveToolbar()
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(fInteractor.getWallNoCache(
            accountId,
            owner_id,
            offset,
            100,
            WallCriteria.MODE_ALL
        )
            .fromIOToMain()
            .subscribe({ data ->
                onActualDataReceived(
                    offset,
                    data
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun update(data: List<Post>) {
        for (i in data) {
            if (i.hasAttachments() && !i.attachments?.audios.isNullOrEmpty()) mAudios.add(i)
            if (i.hasCopyHierarchy()) i.getCopyHierarchy()?.let { update(it) }
        }
    }

    private fun onActualDataReceived(offset: Int, data: List<Post>) {
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (endOfContent) resumedView?.onSetLoadingStatus(2)
        if (offset == 0) {
            loaded = data.size
            mAudios.clear()
            update(data)
            resolveToolbar()
            view?.notifyDataSetChanged()
        } else {
            val startSize = mAudios.size
            loaded += data.size
            update(data)
            resolveToolbar()
            view?.notifyDataAdded(
                startSize,
                mAudios.size - startSize
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(actualDataLoading)
        if (!endOfContent) resumedView?.onSetLoadingStatus(
            if (actualDataLoading) 1 else 0
        )
    }

    private fun resolveToolbar() {
        view?.let {
            it.toolbarTitle(getString(R.string.attachments_in_wall))
            it.toolbarSubtitle(
                getString(
                    R.string.audios_posts_count,
                    safeCountOf(mAudios)
                ) + " " + getString(R.string.posts_analized, loaded)
            )
        }
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData(loaded)
            return false
        }
        return true
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        loadActualData(0)
    }

    fun firePostBodyClick(post: Post) {
        if (intValueIn(post.postType, VKApiPost.Type.SUGGEST, VKApiPost.Type.POSTPONE)) {
            view?.openPostEditor(
                accountId,
                post
            )
            return
        }
        firePostClick(post)
    }

    fun firePostRestoreClick(post: Post) {
        appendDisposable(fInteractor.restore(accountId, post.ownerId, post.vkid)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    fun fireLikeLongClick(post: Post) {
        view?.goToLikes(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireShareLongClick(post: Post) {
        view?.goToReposts(
            accountId,
            "post",
            post.ownerId,
            post.vkid
        )
    }

    fun fireLikeClick(post: Post) {
        appendDisposable(fInteractor.like(accountId, post.ownerId, post.vkid, !post.isUserLikes)
            .fromIOToMain()
            .subscribe(ignore()) { t ->
                showError(t)
            })
    }

    init {
        loadActualData(0)
    }
}