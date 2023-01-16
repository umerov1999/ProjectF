package dev.ragnarok.fenrir.fragment.comments

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.*
import dev.ragnarok.fenrir.model.*

interface ICommentsView : IAttachmentsPlacesView, IMvpView, IErrorView,
    IToolbarView, IProgressView, IToastView {
    fun displayData(data: MutableList<Comment>)
    fun notifyDataSetChanged()
    fun setupLoadUpHeader(@LoadMoreState state: Int)
    fun setupLoadDownFooter(@LoadMoreState state: Int)
    fun notifyDataAddedToTop(count: Int)
    fun notifyDataAddedToBottom(count: Int)
    fun notifyItemChanged(index: Int)
    fun moveFocusTo(index: Int, smooth: Boolean)
    fun displayBody(body: String?)
    fun displayAttachmentsCount(count: Int)
    fun setButtonSendAvailable(available: Boolean)
    fun openAttachmentsManager(
        accountId: Long,
        draftCommentId: Int,
        sourceOwnerId: Long,
        draftCommentBody: String?
    )

    fun setupReplyViews(replyTo: String?)
    fun replaceBodySelectionTextTo(replyText: String?)
    fun goToCommentEdit(accountId: Long, comment: Comment, commemtId: Int?)
    fun goToWallPost(accountId: Long, postId: Int, postOwnerId: Long)
    fun goToVideoPreview(accountId: Long, videoId: Int, videoOwnerId: Long)
    fun banUser(accountId: Long, groupId: Long, user: User)
    fun displayAuthorAvatar(url: String?)
    fun showAuthorSelectDialog(owners: List<Owner>)
    fun scrollToPosition(position: Int)
    fun showCommentSentToast()
    fun setupOptionMenu(
        topicPollAvailable: Boolean,
        gotoSourceAvailable: Boolean,
        @StringRes gotoSourceText: Int?
    )

    fun setEpmtyTextVisible(visible: Boolean)
    fun setCenterProgressVisible(visible: Boolean)
    fun displayDeepLookingCommentProgress()
    fun dismissDeepLookingCommentProgress()
    fun setCanSendSelectAuthor(can: Boolean)
    fun updateStickers(items: List<Sticker>)
    interface ICommentContextView {
        fun setCanEdit(can: Boolean)
        fun setCanDelete(can: Boolean)
        fun setCanBan(can: Boolean)
    }
}