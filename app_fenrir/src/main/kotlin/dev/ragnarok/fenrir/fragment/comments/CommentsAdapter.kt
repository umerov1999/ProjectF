package dev.ragnarok.fenrir.fragment.comments

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder.Companion.forComment
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.link.internal.TopicLink
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.place.PlaceFactory.getCommentsThreadPlace
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.CommentContainer
import dev.ragnarok.fenrir.view.WeakViewAnimatorAdapter
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView
import java.util.Locale

class CommentsAdapter(
    private val context: Context,
    items: MutableList<Comment>,
    attachmentsActionCallback: OnAttachmentsActionCallback
) : RecyclerBindableAdapter<Comment, RecyclerView.ViewHolder>(items) {
    private val attachmentsViewBinder: AttachmentsViewBinder =
        AttachmentsViewBinder(context, attachmentsActionCallback)
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val colorTextSecondary: Int = CurrentTheme.getSecondaryTextColorCode(context)
    private val iconColorActive: Int = CurrentTheme.getColorPrimary(context)
    private var onHashTagClickListener: EmojiconTextView.OnHashTagClickListener? = null
    private var listener: OnCommentActionListener? = null
    fun setOnHashTagClickListener(onHashTagClickListener: EmojiconTextView.OnHashTagClickListener?) {
        this.onHashTagClickListener = onHashTagClickListener
    }

    override fun onBindItemViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int,
        type: Int
    ) {
        when (type) {
            TYPE_NORMAL -> bindNormalHolder(viewHolder as NormalCommentHolder, getItem(position))
            TYPE_DELETED -> bindDeletedComment(viewHolder as DeletedHolder, getItem(position))
        }
    }

    private fun bindDeletedComment(holder: DeletedHolder, comment: Comment) {
        holder.buttonRestore.setOnClickListener {
            listener?.onRestoreComment(comment.getObjectId())
        }
    }

    private fun bindNormalHolder(holder: NormalCommentHolder, comment: Comment) {
        holder.cancelSelectionAnimation()
        if (comment.isAnimationNow) {
            holder.startSelectionAnimation()
            comment.setAnimationNow(false)
        }
        holder.click.setOnLongClickListener {
            listener?.populateCommentContextMenu(comment)
            true
        }
        holder.tvText.setOnLongClickListener {
            listener?.populateCommentContextMenu(comment)
            true
        }
        if (!comment.hasAttachments()) {
            holder.vAttachmentsRoot.visibility = View.GONE
        } else {
            holder.vAttachmentsRoot.visibility = View.VISIBLE
            attachmentsViewBinder.displayAttachments(
                comment.attachments,
                holder.attachmentContainers,
                true,
                null, null, null
            )
        }
        holder.tvOwnerName.text = comment.fullAuthorName
        val text =
            OwnerLinkSpanFactory.withSpans(
                comment.text,
                owners = true,
                topics = true,
                listener = object : LinkActionAdapter() {
                    override fun onTopicLinkClicked(link: TopicLink) {
                        listener?.onReplyToOwnerClick(link.replyToOwner, link.replyToCommentId)
                    }

                    override fun onOwnerClick(ownerId: Long) {
                        listener?.onAvatarClick(ownerId)
                    }
                })
        val hasOpenButton =
            comment.threadsCount > 0 && comment.threadsCount > comment.receivedThreadsCount()
        holder.ivOpenThread.visibility = if (hasOpenButton) View.VISIBLE else View.GONE
        if (hasOpenButton) {
            holder.ivOpenThread.setOnClickListener {
                getCommentsThreadPlace(
                    Settings.get().accounts().current,
                    comment.commented,
                    null,
                    comment.getObjectId()
                ).tryOpenWith(
                    context
                )
            }
            holder.ivOpenThread.text = context.getString(
                R.string.open_comment_thread, AppTextUtils.getCounterWithK(
                    comment.threadsCount
                )
            )
        } else if (comment.threadsCount > 0) {
            holder.item_comment_thread_counter.setOnClickListener {
                getCommentsThreadPlace(
                    Settings.get().accounts().current,
                    comment.commented,
                    null,
                    comment.getObjectId()
                ).tryOpenWith(
                    context
                )
            }
            holder.item_comment_thread_counter.text =
                AppTextUtils.getCounterWithK(comment.threadsCount)
        }
        holder.item_comment_thread_counter.visibility =
            if (comment.threadsCount > 0 && !hasOpenButton) View.VISIBLE else View.GONE
        holder.threads.visibility = if (comment.threadsCount > 0) View.VISIBLE else View.GONE
        holder.threads.displayComments(
            comment.threads,
            attachmentsViewBinder,
            listener,
            onHashTagClickListener
        )
        if (text.isNullOrEmpty() && comment.fromId == 0L) {
            holder.tvText.visibility = View.VISIBLE
            holder.tvText.setText(R.string.deleted)
        } else {
            holder.tvText.setText(text, TextView.BufferType.SPANNABLE)
            holder.tvText.visibility =
                if (comment.text.isNullOrEmpty()) View.GONE else View.VISIBLE
            holder.tvText.movementMethod = LinkMovementMethod.getInstance()
        }
        holder.tvLikeCounter.visibility = if (comment.likesCount > 0) View.VISIBLE else View.GONE
        TextViewCompat.setCompoundDrawableTintList(
            holder.tvLikeCounter,
            ColorStateList.valueOf(if (comment.isUserLikes) iconColorActive else colorTextSecondary)
        )
        holder.tvLikeCounter.text = AppTextUtils.getCounterWithK(comment.likesCount)
        holder.tvLikeCounter.visibility = if (comment.likesCount > 0) View.VISIBLE else View.GONE
        holder.tvLikeCounter.setTextColor(if (comment.isUserLikes) iconColorActive else colorTextSecondary)
        holder.tvTime.movementMethod = LinkMovementMethod.getInstance()
        displayAvatar(
            holder.ivOwnerAvatar,
            transformation,
            comment.maxAuthorAvaUrl,
            Constants.PICASSO_TAG
        )
        holder.tvTime.setText(genTimeAndReplyText(comment), TextView.BufferType.SPANNABLE)
        holder.tvTime.setTextColor(colorTextSecondary)
        holder.tvLikeCounter.setOnClickListener {
            listener?.onCommentLikeClick(comment, !comment.isUserLikes)
        }
        holder.ivOwnerAvatar.setOnClickListener {
            if (comment.fromId == 0L) {
                return@setOnClickListener
            }
            listener?.onAvatarClick(comment.fromId)
        }
    }

    private fun genTimeAndReplyText(comment: Comment): Spannable {
        val time = AppTextUtils.getDateFromUnixTime(comment.date)
        if (comment.replyToUser == 0L) {
            return Spannable.Factory.getInstance().newSpannable(time)
        }
        val commentText = context.getString(R.string.comment).lowercase(Locale.getDefault())
        val target = context.getString(R.string.in_response_to, time, commentText)
        val start = target.indexOf(commentText)
        val spannable = Spannable.Factory.getInstance().newSpannable(target)
        val span: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                listener?.onReplyToOwnerClick(comment.replyToUser, comment.replyToComment)
            }
        }
        spannable.setSpan(span, start, target.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spannable
    }

    override fun viewHolder(view: View, type: Int): RecyclerView.ViewHolder {
        return when (type) {
            TYPE_NORMAL -> NormalCommentHolder(view)
            TYPE_DELETED -> DeletedHolder(
                view
            )

            else -> throw IllegalArgumentException()
        }
    }

    override fun layoutId(type: Int): Int {
        when (type) {
            TYPE_DELETED -> return R.layout.item_comment_deleted
            TYPE_NORMAL -> return R.layout.item_comment
        }
        throw IllegalArgumentException()
    }

    fun setListener(listener: OnCommentActionListener?) {
        this.listener = listener
    }

    override fun getItemType(position: Int): Int {
        return if (getItem(position - headersCount).isDeleted) TYPE_DELETED else TYPE_NORMAL
    }

    interface OnCommentActionListener {
        fun onReplyToOwnerClick(ownerId: Long, commentId: Int)
        fun onRestoreComment(commentId: Int)
        fun onAvatarClick(ownerId: Long)
        fun onCommentLikeClick(comment: Comment, add: Boolean)
        fun populateCommentContextMenu(comment: Comment)
    }

    private class DeletedHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val buttonRestore: MaterialButton = itemView.findViewById(R.id.item_comment_deleted_restore)

    }

    private inner class NormalCommentHolder(root: View) :
        RecyclerView.ViewHolder(root) {
        val tvOwnerName: TextView = root.findViewById(R.id.item_comment_owner_name)
        val ivOwnerAvatar: ImageView = root.findViewById(R.id.item_comment_owner_avatar)
        val tvText: EmojiconTextView = root.findViewById(R.id.item_comment_text)
        val tvTime: TextView
        val tvLikeCounter: TextView
        val selectionView: View
        val vAttachmentsRoot: View
        val ivOpenThread: MaterialButton
        val threads: CommentContainer
        val click: ViewGroup
        val item_comment_thread_counter: TextView
        val attachmentContainers: AttachmentsHolder
        val animationAdapter: Animator.AnimatorListener
        var animator: ObjectAnimator? = null
        fun startSelectionAnimation() {
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, View.ALPHA, 0.0f)
            animator?.duration = 1500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun cancelSelectionAnimation() {
            animator?.cancel()
            animator = null
            selectionView.visibility = View.INVISIBLE
        }

        init {
            tvText.setOnHashTagClickListener(object : EmojiconTextView.OnHashTagClickListener {
                override fun onHashTagClicked(hashTag: String) {
                    onHashTagClickListener?.onHashTagClicked(hashTag)
                }
            })
            ivOpenThread = root.findViewById(R.id.item_open_threads)
            item_comment_thread_counter = root.findViewById(R.id.item_comment_thread_counter)
            tvTime = root.findViewById(R.id.item_comment_time)
            tvLikeCounter = root.findViewById(R.id.item_comment_like_counter)
            selectionView = root.findViewById(R.id.item_comment_selection)
            selectionView.setBackgroundColor(CurrentTheme.getColorPrimary(context))
            TextViewCompat.setCompoundDrawableTintList(
                tvLikeCounter,
                ColorStateList.valueOf(CurrentTheme.getSecondaryTextColorCode(tvLikeCounter.context))
            )
            vAttachmentsRoot = root.findViewById(R.id.item_comment_attachments_root)
            threads = root.findViewById(R.id.item_comment_threads)
            click = root.findViewById(R.id.comment_click_container)
            attachmentContainers = forComment((vAttachmentsRoot as ViewGroup))
            animationAdapter = object : WeakViewAnimatorAdapter<View>(selectionView) {
                override fun onAnimationEnd(view: View) {
                    view.visibility = View.INVISIBLE
                }

                override fun onAnimationStart(view: View) {
                    view.visibility = View.VISIBLE
                }

                override fun onAnimationCancel(view: View) {
                    view.visibility = View.INVISIBLE
                }
            }
        }
    }

    companion object {
        private const val TYPE_DELETED = 0
        private const val TYPE_NORMAL = 1
    }

}