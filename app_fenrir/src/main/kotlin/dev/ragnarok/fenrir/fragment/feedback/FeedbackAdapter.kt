package dev.ragnarok.fenrir.fragment.feedback

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.fragment.feedback.FeedbackAdapter.FeedbackHolder
import dev.ragnarok.fenrir.model.feedback.CommentFeedback
import dev.ragnarok.fenrir.model.feedback.CopyFeedback
import dev.ragnarok.fenrir.model.feedback.Feedback
import dev.ragnarok.fenrir.model.feedback.FeedbackType
import dev.ragnarok.fenrir.model.feedback.LikeCommentFeedback
import dev.ragnarok.fenrir.model.feedback.LikeFeedback
import dev.ragnarok.fenrir.model.feedback.MentionCommentFeedback
import dev.ragnarok.fenrir.model.feedback.MentionFeedback
import dev.ragnarok.fenrir.model.feedback.PostPublishFeedback
import dev.ragnarok.fenrir.model.feedback.ReplyCommentFeedback
import dev.ragnarok.fenrir.model.feedback.UsersFeedback
import dev.ragnarok.fenrir.view.OnlineView
import java.util.Calendar

class FeedbackAdapter(
    context: Activity,
    items: MutableList<Feedback>,
    attachmentsActionCallback: OnAttachmentsActionCallback
) : RecyclerBindableAdapter<Feedback, FeedbackHolder>(items) {
    private val mContext: Context
    private val mFeedbackViewBinder: FeedbackViewBinder
    private var mClickListener: ClickListener? = null
    override fun onBindItemViewHolder(
        viewHolder: FeedbackHolder,
        position: Int,
        type: Int
    ) {
        val item = getItem(position)
        val previous = if (position > 0) getItem(position - 1) else null
        when (getHeaderStatus(previous, item.date)) {
            HEADER_DISABLE -> viewHolder.headerRoot.visibility = View.GONE
            HEADER_OLD -> {
                viewHolder.headerRoot.visibility = View.VISIBLE
                viewHolder.headerText.text = mContext.getString(R.string.dialog_day_older)
            }

            HEADER_TODAY -> {
                viewHolder.headerRoot.visibility = View.VISIBLE
                viewHolder.headerText.text = mContext.getString(R.string.dialog_day_today)
            }

            HEADER_YESTERDAY -> {
                viewHolder.headerRoot.visibility = View.VISIBLE
                viewHolder.headerText.text = mContext.getString(R.string.dialog_day_yesterday)
            }

            HEADER_THIS_WEEK -> {
                viewHolder.headerRoot.visibility = View.VISIBLE
                viewHolder.headerText.text = mContext.getString(R.string.dialog_day_ten_days)
            }
        }
        if (viewHolder is CommentHolder) {
            configCommentHolder(item, viewHolder)
        }
        if (viewHolder is UsersHolder) {
            configUserHolder(item, viewHolder)
        }
        viewHolder.contentRoot.setOnClickListener {
            mClickListener?.onNotificationClick(item)
        }
    }

    fun setClickListener(clickListener: ClickListener?) {
        mClickListener = clickListener
    }

    private fun configCommentHolder(notification: Feedback, holder: CommentHolder) {
        when (notification.type) {
            FeedbackType.WALL_PUBLISH -> mFeedbackViewBinder.configWallPublishFeedback(
                notification as PostPublishFeedback,
                holder
            )

            FeedbackType.WALL -> mFeedbackViewBinder.configWallFeedback(
                notification as PostPublishFeedback,
                holder
            )

            FeedbackType.MENTION -> mFeedbackViewBinder.configMentionFeedback(
                notification as MentionFeedback,
                holder
            )

            FeedbackType.REPLY_COMMENT -> mFeedbackViewBinder.configReplyCommentFeedback(
                notification as ReplyCommentFeedback,
                holder
            )

            FeedbackType.REPLY_TOPIC -> mFeedbackViewBinder.configReplyTopicFeedback(
                notification as ReplyCommentFeedback,
                holder
            )

            FeedbackType.REPLY_COMMENT_PHOTO -> mFeedbackViewBinder.configReplyCommentPhotoFeedback(
                notification as ReplyCommentFeedback,
                holder
            )

            FeedbackType.REPLY_COMMENT_VIDEO -> mFeedbackViewBinder.configReplyCommentVideoFeedback(
                notification as ReplyCommentFeedback,
                holder
            )

            FeedbackType.MENTION_COMMENT_POST -> mFeedbackViewBinder.configMentionCommentsFeedback(
                notification as MentionCommentFeedback,
                holder
            )

            FeedbackType.MENTION_COMMENT_PHOTO -> mFeedbackViewBinder.configMentionCommentsPhotoFeedback(
                notification as MentionCommentFeedback,
                holder
            )

            FeedbackType.MENTION_COMMENT_VIDEO -> mFeedbackViewBinder.configMentionCommentsVideoFeedback(
                notification as MentionCommentFeedback,
                holder
            )

            FeedbackType.COMMENT_POST -> mFeedbackViewBinder.configCommentPostFeedback(
                notification as CommentFeedback,
                holder
            )

            FeedbackType.COMMENT_PHOTO -> mFeedbackViewBinder.configCommentPhotoFeedback(
                notification as CommentFeedback,
                holder
            )

            FeedbackType.COMMENT_VIDEO -> mFeedbackViewBinder.configCommentVideoFeedback(
                notification as CommentFeedback,
                holder
            )
        }
    }

    private fun configUserHolder(notification: Feedback, holder: UsersHolder) {
        when (notification.type) {
            FeedbackType.LIKE_POST -> mFeedbackViewBinder.configLikePostFeedback(
                notification as LikeFeedback,
                holder
            )

            FeedbackType.LIKE_PHOTO -> mFeedbackViewBinder.configLikePhotoFeedback(
                notification as LikeFeedback,
                holder
            )

            FeedbackType.LIKE_VIDEO -> mFeedbackViewBinder.configLikeVideoFeedback(
                notification as LikeFeedback,
                holder
            )

            FeedbackType.LIKE_COMMENT_POST -> mFeedbackViewBinder.configLikeCommentFeedback(
                notification as LikeCommentFeedback,
                holder
            )

            FeedbackType.LIKE_COMMENT_TOPIC -> mFeedbackViewBinder.configLikeCommentTopicFeedback(
                notification as LikeCommentFeedback,
                holder
            )

            FeedbackType.FOLLOW -> mFeedbackViewBinder.configFollowFeedback(
                notification as UsersFeedback,
                holder
            )

            FeedbackType.FRIEND_ACCEPTED -> mFeedbackViewBinder.configFriendAcceptedFeedback(
                notification as UsersFeedback,
                holder
            )

            FeedbackType.LIKE_COMMENT_PHOTO -> mFeedbackViewBinder.configLikeCommentForPhotoFeedback(
                notification as LikeCommentFeedback,
                holder
            )

            FeedbackType.LIKE_COMMENT_VIDEO -> mFeedbackViewBinder.configLikeCommentVideoFeedback(
                notification as LikeCommentFeedback,
                holder
            )

            FeedbackType.COPY_POST -> mFeedbackViewBinder.configCopyPostFeedback(
                notification as CopyFeedback,
                holder
            )

            FeedbackType.COPY_PHOTO -> mFeedbackViewBinder.configCopyPhotoFeedback(
                notification as CopyFeedback,
                holder
            )

            FeedbackType.COPY_VIDEO -> mFeedbackViewBinder.configCopyVideoFeedback(
                notification as CopyFeedback,
                holder
            )
        }
    }

    private fun getItemViewType(notification: Feedback): Int {
        when (notification.type) {
            FeedbackType.WALL, FeedbackType.WALL_PUBLISH, FeedbackType.MENTION, FeedbackType.COMMENT_POST, FeedbackType.MENTION_COMMENT_POST, FeedbackType.COMMENT_PHOTO, FeedbackType.MENTION_COMMENT_VIDEO, FeedbackType.MENTION_COMMENT_PHOTO, FeedbackType.COMMENT_VIDEO, FeedbackType.REPLY_COMMENT, FeedbackType.REPLY_COMMENT_PHOTO, FeedbackType.REPLY_COMMENT_VIDEO, FeedbackType.REPLY_TOPIC -> return TYPE_COMMENTS
            FeedbackType.FOLLOW, FeedbackType.FRIEND_ACCEPTED, FeedbackType.COPY_POST, FeedbackType.COPY_PHOTO, FeedbackType.COPY_VIDEO, FeedbackType.LIKE_POST, FeedbackType.LIKE_PHOTO, FeedbackType.LIKE_VIDEO, FeedbackType.LIKE_COMMENT_POST, FeedbackType.LIKE_COMMENT_PHOTO, FeedbackType.LIKE_COMMENT_VIDEO, FeedbackType.LIKE_COMMENT_TOPIC -> return TYPE_USERS
        }
        throw IllegalArgumentException("Invalid feedback type: " + notification.type)
    }

    override fun getItemType(position: Int): Int {
        val notification = getItem(position - headersCount)
        return getItemViewType(notification)
    }

    override fun viewHolder(view: View, type: Int): FeedbackHolder {
        when (type) {
            TYPE_COMMENTS -> return CommentHolder(view)
            TYPE_USERS -> return UsersHolder(view)
        }
        throw UnsupportedOperationException()
    }

    override fun layoutId(type: Int): Int {
        when (type) {
            TYPE_COMMENTS -> return R.layout.item_feedback_comment
            TYPE_USERS -> return R.layout.item_feedback_user
        }
        throw UnsupportedOperationException()
    }

    private fun getHeaderStatus(previous: Feedback?, date: Long): Int {
        var previousDate: Long? = null
        if (previous != null) {
            previousDate = previous.date * 1000
        }
        val stCurrent = getStatus(date * 1000)
        return if (previousDate == null) {
            stCurrent
        } else {
            val stPrevious = getStatus(previousDate)
            if (stCurrent == stPrevious) {
                HEADER_DISABLE
            } else {
                stCurrent
            }
        }
    }

    private fun getStatus(time: Long): Int {
        val current = Calendar.getInstance()
        current[current[Calendar.YEAR], current[Calendar.MONTH], current[Calendar.DATE], 0, 0] = 0
        val today = current.timeInMillis
        val yesterday = today - 24 * 60 * 60 * 1000
        val week = today - 10 * 24 * 60 * 60 * 1000
        return when {
            time >= today -> {
                HEADER_TODAY
            }

            time >= yesterday -> {
                HEADER_YESTERDAY
            }

            time >= week -> {
                HEADER_THIS_WEEK
            }

            else -> HEADER_OLD
        }
    }

    interface ClickListener {
        fun onNotificationClick(notification: Feedback)
    }

    open class FeedbackHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerRoot: View = itemView.findViewById(R.id.header_root)
        val contentRoot: View = itemView.findViewById(R.id.content_root)
        val headerText: TextView = itemView.findViewById(R.id.item_feedback_header_title)

    }

    class UsersHolder(root: View) : FeedbackHolder(root) {
        val uAvatar: ImageView = root.findViewById(R.id.item_friend_avatar)
        val uName: TextView = root.findViewById(R.id.item_friend_name)
        val uInfo: TextView = root.findViewById(R.id.item_additional_info)
        val uTime: TextView = root.findViewById(R.id.item_friend_time)
        val uChangable: OnlineView = root.findViewById(R.id.item_circle_friend_changable)
        val ivAttachment: ImageView = root.findViewById(R.id.item_feedback_user_attachment)
    }

    class CommentHolder(root: View) : FeedbackHolder(root) {
        val cOwnerAvatar: ImageView = root.findViewById(R.id.item_comment_owner_avatar)
        val cChangable: OnlineView = root.findViewById(R.id.item_circle_changable)
        val cOwnerName: TextView = root.findViewById(R.id.item_comment_owner_name)
        val cOwnerText: TextView = root.findViewById(R.id.item_comment_text)
        val cOwnerTime: TextView = root.findViewById(R.id.item_comment_time)
        val cReplyOwnerAvatar: ImageView = root.findViewById(R.id.item_comment_reply_owner_avatar)
        val cReplyName: TextView = root.findViewById(R.id.item_comment_reply_owner_name)
        val cReplyText: TextView = root.findViewById(R.id.item_comment_reply_text)
        val cReplyTime: TextView = root.findViewById(R.id.item_comment_reply_time)
        val cReplyContainer: ViewGroup = root.findViewById(R.id.comment_reply_feedback)
        val vAttachmentsRoot: View = root.findViewById(R.id.item_feedback_comment_attachments_root)
        val vReplyAttachmentsRoot: View =
            root.findViewById(R.id.item_reply_comment_attachments_root)
        val ivRightAttachment: ImageView = root.findViewById(R.id.item_feedback_comment_attachment)

    }

    companion object {
        private const val TYPE_COMMENTS = 0
        private const val TYPE_USERS = 1
        private const val HEADER_DISABLE = 0
        private const val HEADER_TODAY = 1
        private const val HEADER_THIS_WEEK = 2
        private const val HEADER_OLD = 3
        private const val HEADER_YESTERDAY = 4
    }

    init {
        mContext = context
        mFeedbackViewBinder = FeedbackViewBinder(context, attachmentsActionCallback)
    }
}