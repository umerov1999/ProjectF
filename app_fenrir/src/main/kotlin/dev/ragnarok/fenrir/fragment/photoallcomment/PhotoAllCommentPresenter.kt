package dev.ragnarok.fenrir.fragment.photoallcomment

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Includes.networkInterfaces
import dev.ragnarok.fenrir.Includes.stores
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.domain.ICommentsInteractor
import dev.ragnarok.fenrir.domain.IPhotosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.domain.impl.CommentsInteractor
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.AccessIdPair
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.WallReply
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.DisposableHolder
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.rxutils.RxUtils.dummy
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PhotoAllCommentPresenter(
    accountId: Long,
    private val owner_id: Long,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<IPhotoAllCommentView>(accountId, savedInstanceState) {
    private val photosInteractor: IPhotosInteractor = InteractorFactory.createPhotosInteractor()
    private val interactor: ICommentsInteractor
    private val mComments: ArrayList<Comment>
    private val netDisposable = CompositeDisposable()
    private val deepLookingHolder = DisposableHolder<Void>()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        deepLookingHolder.dispose()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(photosInteractor.getAllComments(
            accountId,
            owner_id,
            null,
            offset,
            COUNT_PER_REQUEST
        )
            .fromIOToMain()
            .subscribe({
                onNetDataReceived(
                    offset,
                    it
                )
            }) { t -> onNetDataGetError(t) })
    }

    private fun onNetDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onNetDataReceived(offset: Int, comments: List<Comment>) {
        cacheLoadingNow = false
        mEndOfContent = comments.isEmpty()
        netLoadingNow = false
        if (offset == 0) {
            mComments.clear()
            mComments.addAll(comments)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mComments.size
            mComments.addAll(comments)
            view?.notifyDataAdded(
                startSize,
                comments.size
            )
        }
        resolveRefreshingView()
    }

    private fun requestAtLast() {
        request(0)
    }

    private fun requestNext() {
        request(mComments.size)
    }

    override fun onGuiCreated(viewHost: IPhotoAllCommentView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mComments)
        resolveRefreshingView()
    }

    private fun canLoadMore(): Boolean {
        return mComments.isNotEmpty() && !cacheLoadingNow && !netLoadingNow && !mEndOfContent
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        requestAtLast()
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    fun fireCommentLikeClick(comment: Comment, add: Boolean) {
        likeInternal(add, comment)
    }

    fun fireGoPhotoClick(comment: Comment) {
        appendDisposable(photosInteractor.getPhotosByIds(
            accountId,
            listOf(AccessIdPair(comment.commented.sourceId, owner_id, null))
        )
            .fromIOToMain()
            .subscribe({
                view?.openSimplePhotoGallery(
                    accountId,
                    ArrayList(it),
                    0,
                    false
                )
            }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    private fun likeInternal(add: Boolean, comment: Comment) {
        if (Settings.get().other().isDisable_likes || Utils.isHiddenAccount(
                accountId
            )
        ) {
            return
        }
        appendDisposable(interactor.like(accountId, comment.commented, comment.getObjectId(), add)
            .fromIOToMain()
            .subscribe(dummy()) { t ->
                showError(t)
            })
    }

    fun fireReport(comment: Comment, context: Context) {
        val items = arrayOf<CharSequence>(
            "Спам",
            "Детская порнография",
            "Экстремизм",
            "Насилие",
            "Пропаганда наркотиков",
            "Материал для взрослых",
            "Оскорбление",
            "Призывы к суициду"
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.report)
            .setItems(items) { dialog: DialogInterface, item: Int ->
                appendDisposable(
                    interactor.reportComment(
                        accountId,
                        comment.fromId,
                        comment.getObjectId(),
                        item
                    )
                        .fromIOToMain()
                        .subscribe({ p ->
                            if (p == 1) view?.customToast?.showToast(
                                R.string.success
                            )
                            else view?.customToast?.showToast(R.string.error)
                        }, { t ->
                            showError(getCauseIfRuntime(t))
                        })
                )
                dialog.dismiss()
            }
            .show()
    }

    fun fireWhoLikesClick(comment: Comment) {
        view?.goToLikes(
            accountId,
            "photo_comment",
            comment.commented.sourceOwnerId,
            comment.getObjectId()
        )
    }

    fun fireReplyToChat(comment: Comment, context: Context) {
        startForSendAttachments(
            context,
            accountId,
            WallReply().buildFromComment(comment, comment.commented)
        )
    }

    fun fireReplyToOwnerClick(commentId: Int) {
        for (y in mComments.indices) {
            val comment = mComments[y]
            if (comment.getObjectId() == commentId) {
                comment.setAnimationNow(true)
                view?.let {
                    it.notifyItemChanged(y)
                    it.moveFocusTo(y, true)
                }
                return
            }
        }

        //safeShowToast(getView(), R.string.the_comment_is_not_in_the_list, false);
        startDeepCommentFinding(commentId)
    }

    private val firstCommentInList: Comment?
        get() = if (mComments.nonNullNoEmpty()) mComments[mComments.size - 1] else null

    private fun startDeepCommentFinding(commentId: Int) {
        if (netLoadingNow || cacheLoadingNow) {
            // не грузить, если сейчас что-то грузится
            return
        }
        val older = firstCommentInList
        view?.displayDeepLookingCommentProgress()
        deepLookingHolder.append(older?.getObjectId()?.let { it1 ->
            interactor.getAllCommentsRange(
                accountId,
                older.commented,
                it1,
                commentId
            )
                .fromIOToMain()
                .subscribe({
                    onDeepCommentLoadingResponse(
                        commentId,
                        it
                    )
                }) { throwable -> onDeepCommentLoadingError(throwable) }
        })
    }

    private fun onDeepCommentLoadingError(throwable: Throwable) {
        view?.dismissDeepLookingCommentProgress()
        if (throwable is NotFoundException) {
            view?.customToast?.showToast(
                R.string.the_comment_is_not_in_the_list
            )
        } else {
            showError(throwable)
        }
    }

    private fun onDeepCommentLoadingResponse(commentId: Int, comments: List<Comment>) {
        view?.dismissDeepLookingCommentProgress()
        mComments.addAll(comments)
        var index = -1
        for (i in mComments.indices) {
            val comment = mComments[i]
            if (comment.getObjectId() == commentId) {
                index = i
                comment.setAnimationNow(true)
                break
            }
        }
        if (index == -1) {
            return
        }
        view?.notifyDataAddedToTop(
            comments.size
        )
        val finalIndex = index
        view?.moveFocusTo(
            finalIndex,
            false
        )
    }

    public override fun onGuiDestroyed() {
        deepLookingHolder.dispose()
        super.onGuiDestroyed()
    }

    fun fireDeepLookingCancelledByUser() {
        deepLookingHolder.dispose()
    }

    companion object {
        private const val COUNT_PER_REQUEST = 25
    }

    init {
        interactor = CommentsInteractor(networkInterfaces, stores, owners)
        mComments = ArrayList()
        requestAtLast()
    }
}