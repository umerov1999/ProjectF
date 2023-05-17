package dev.ragnarok.fenrir.fragment.wall.wallattachments.wallsearchcommentsattachments

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.ICommentsInteractor
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.CommentsInteractor
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.CommentedType
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.disposables.CompositeDisposable

class WallSearchCommentsAttachmentsPresenter(
    accountId: Long,
    private val owner_id: Long,
    private val posts: List<Int>,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<IWallSearchCommentsAttachmentsView>(accountId, savedInstanceState) {
    private val data: MutableList<Comment>
    private val interactor: ICommentsInteractor
    private val actualDataDisposable = CompositeDisposable()
    private var loaded = 0
    private var offset = 0
    private var index = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    override fun onGuiCreated(viewHost: IWallSearchCommentsAttachmentsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
        resolveToolbar()
    }

    private fun loadActualData() {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(interactor.getCommentsNoCache(
            accountId,
            owner_id,
            posts[index],
            offset
        )
            .fromIOToMain()
            .subscribe({ onActualDataReceived(it) }) {
                onActualDataGetError(
                    it
                )
            })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun onActualDataReceived(res: List<Comment>) {
        offset += 100
        actualDataLoading = false
        endOfContent = res.isEmpty() && index == posts.size - 1
        if (res.isEmpty()) {
            index++
            offset = 0
        }
        actualDataReceived = true
        if (endOfContent) resumedView?.onSetLoadingStatus(
            2
        )
        val startSize = data.size
        loaded += res.size
        data.addAll(res)
        resolveToolbar()
        view?.notifyDataAdded(
            startSize,
            res.size - startSize
        )
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
        if (!endOfContent) resumedView?.onSetLoadingStatus(
            if (actualDataLoading) 1 else 0
        )
    }

    private fun resolveToolbar() {
        view?.let {
            it.toolbarTitle(getString(R.string.attachments_in_wall))
            it.toolbarSubtitle(
                getString(
                    R.string.comments,
                    safeCountOf(data)
                ) + " " + getString(R.string.comments_analized, loaded)
            )
        }
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd(button: Boolean): Boolean {
        if (!endOfContent && actualDataReceived && !actualDataLoading) {
            loadActualData()
            return false
        }
        if (button && endOfContent) resumedView?.onSetLoadingStatus(2)
        return true
    }

    fun fireRefresh() {
        actualDataDisposable.clear()
        actualDataLoading = false
        offset = 0
        index = 0
        loaded = 0
        data.clear()
        view?.notifyDataSetChanged()
        loadActualData()
    }

    fun fireReplyToOwnerClick(commentId: Int) {
        for (y in data.indices) {
            val comment = data[y]
            if (comment.getObjectId() == commentId) {
                comment.setAnimationNow(true)
                view?.notifyItemChanged(
                    y
                )
                view?.moveFocusTo(y)
                return
            } else if (comment.hasThreads()) {
                for (s in comment.threads.orEmpty()) {
                    if (s.getObjectId() == commentId) {
                        s.setAnimationNow(true)
                        view?.notifyItemChanged(
                            y
                        )
                        view?.moveFocusTo(y)
                        return
                    }
                }
            }
        }
    }

    private fun getApiCommentType(comment: Comment): String {
        return when (comment.commented.sourceType) {
            CommentedType.PHOTO -> "photo_comment"
            CommentedType.POST -> "comment"
            CommentedType.VIDEO -> "video_comment"
            CommentedType.TOPIC -> "topic_comment"
            else -> throw IllegalArgumentException()
        }
    }

    fun fireWhoLikesClick(comment: Comment) {
        view?.goToLikes(
            accountId,
            getApiCommentType(comment),
            owner_id,
            comment.getObjectId()
        )
    }

    fun fireGoCommentPostClick(comment: Comment) {
        view?.goToPost(
            accountId,
            comment.commented.sourceOwnerId,
            comment.commented.sourceId
        )
    }

    init {
        data = ArrayList()
        interactor = CommentsInteractor(networkInterfaces, stores, owners)
        loadActualData()
    }
}