package dev.ragnarok.fenrir.view

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder.Companion.forComment
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.comments.CommentsAdapter.OnCommentActionListener
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.link.internal.TopicLink
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.transforms.RoundTransformation
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView
import java.util.*

class CommentContainer : LinearLayout {
    private var transformation: Transformation? = null
    private var colorTextSecondary = 0
    private var iconColorActive = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        transformation = if (isInEditMode) {
            RoundTransformation()
        } else {
            CurrentTheme.createTransformationForAvatar()
        }
        colorTextSecondary = CurrentTheme.getSecondaryTextColorCode(context)
        iconColorActive = CurrentTheme.getColorPrimary(context)
    }

    fun displayComments(
        commentsData: List<Comment>?,
        binder: AttachmentsViewBinder,
        listener: OnCommentActionListener?,
        onHashTagClickListener: EmojiconTextView.OnHashTagClickListener?
    ) {
        if (commentsData == null || commentsData.isEmpty()) {
            visibility = View.GONE
            return
        }
        val comments: MutableList<Comment> = ArrayList()
        if (commentsData.nonNullNoEmpty()) {
            for (i in commentsData) {
                if (!i.isDeleted) {
                    comments.add(i)
                }
            }
        }
        if (comments.isEmpty()) {
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE
        val i = comments.size - childCount
        for (j in 0 until i) {
            val itemView =
                LayoutInflater.from(context).inflate(R.layout.item_comment_container, this, false)
            val holder = CommentHolder(itemView, onHashTagClickListener)
            itemView.tag = holder
            addView(itemView)
        }
        for (g in 0 until childCount) {
            val root = getChildAt(g) as ViewGroup? ?: continue
            if (g < comments.size) {
                val comment = comments[g]
                val check: CommentHolder = root.tag as CommentHolder? ?: continue
                root.setOnLongClickListener {
                    listener?.populateCommentContextMenu(comment)
                    true
                }
                check.cancelSelectionAnimation()
                if (comment.isAnimationNow) {
                    check.startSelectionAnimation()
                    comment.setAnimationNow(false)
                }
                check.tvText.setOnLongClickListener {
                    listener?.populateCommentContextMenu(comment)
                    true
                }
                if (!comment.hasAttachments()) {
                    check.vAttachmentsRoot.visibility = GONE
                } else {
                    check.vAttachmentsRoot.visibility = VISIBLE
                    binder.displayAttachments(
                        comment.attachments,
                        check.attachmentContainers,
                        true,
                        null, null
                    )
                }
                check.tvOwnerName.text = comment.fullAuthorName
                val text = OwnerLinkSpanFactory.withSpans(
                    comment.text,
                    owners = true,
                    topics = true,
                    listener = object : LinkActionAdapter() {
                        override fun onTopicLinkClicked(link: TopicLink) {
                            listener?.onReplyToOwnerClick(link.replyToOwner, link.replyToCommentId)
                        }

                        override fun onOwnerClick(ownerId: Int) {
                            listener?.onAvatarClick(ownerId)
                        }
                    })
                if (text.isNullOrEmpty() && comment.fromId == 0) {
                    check.tvText.visibility = VISIBLE
                    check.tvText.setText(R.string.deleted)
                } else {
                    check.tvText.setText(text, TextView.BufferType.SPANNABLE)
                    check.tvText.visibility =
                        if (comment.text.isNullOrEmpty()) GONE else VISIBLE
                    check.tvText.movementMethod = LinkMovementMethod.getInstance()
                }
                check.tvLikeCounter.visibility = if (comment.likesCount > 0) VISIBLE else GONE
                TextViewCompat.setCompoundDrawableTintList(
                    check.tvLikeCounter,
                    ColorStateList.valueOf(if (comment.isUserLikes) iconColorActive else colorTextSecondary)
                )
                check.tvLikeCounter.text = AppTextUtils.getCounterWithK(comment.likesCount)
                check.tvLikeCounter.visibility = if (comment.likesCount > 0) VISIBLE else GONE
                check.tvLikeCounter.setTextColor(if (comment.isUserLikes) iconColorActive else colorTextSecondary)
                check.tvTime.movementMethod = LinkMovementMethod.getInstance()
                displayAvatar(
                    check.ivOwnerAvatar,
                    transformation,
                    comment.maxAuthorAvaUrl,
                    Constants.PICASSO_TAG
                )
                check.tvTime.setText(
                    genTimeAndReplyText(comment, listener),
                    TextView.BufferType.SPANNABLE
                )
                check.tvTime.setTextColor(colorTextSecondary)
                check.tvLikeCounter.setOnClickListener {
                    listener?.onCommentLikeClick(
                        comment, !comment.isUserLikes
                    )
                }
                check.ivOwnerAvatar.setOnClickListener {
                    if (comment.fromId == 0) {
                        return@setOnClickListener
                    }
                    listener?.onAvatarClick(comment.fromId)
                }
                root.visibility = VISIBLE
            } else {
                root.visibility = GONE
            }
        }
    }

    private fun genTimeAndReplyText(
        comment: Comment,
        listener: OnCommentActionListener?
    ): Spannable {
        val time = AppTextUtils.getDateFromUnixTime(comment.date)
        if (comment.replyToUser == 0) {
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

    private inner class CommentHolder(
        root: View,
        onHashTagClickListener: EmojiconTextView.OnHashTagClickListener?
    ) {
        val tvOwnerName: TextView = root.findViewById(R.id.item_comment_owner_name)
        val ivOwnerAvatar: ImageView = root.findViewById(R.id.item_comment_owner_avatar)
        val tvText: EmojiconTextView = root.findViewById(R.id.item_comment_text)
        val tvTime: TextView
        val tvLikeCounter: TextView
        val vAttachmentsRoot: View
        val selectionView: View
        val attachmentContainers: AttachmentsHolder
        val animationAdapter: Animator.AnimatorListener
        var animator: ObjectAnimator? = null
        fun startSelectionAnimation() {
            selectionView.alpha = 0.5f
            animator = ObjectAnimator.ofFloat(selectionView, ALPHA, 0.0f)
            animator?.duration = 1500
            animator?.addListener(animationAdapter)
            animator?.start()
        }

        fun cancelSelectionAnimation() {
            animator?.cancel()
            animator = null
            selectionView.visibility = INVISIBLE
        }

        init {
            tvText.setOnHashTagClickListener(object : EmojiconTextView.OnHashTagClickListener {
                override fun onHashTagClicked(hashTag: String) {
                    onHashTagClickListener?.onHashTagClicked(hashTag)
                }
            })
            tvTime = root.findViewById(R.id.item_comment_time)
            tvLikeCounter = root.findViewById(R.id.item_comment_like_counter)
            selectionView = root.findViewById(R.id.item_comment_selection)
            selectionView.setBackgroundColor(CurrentTheme.getColorPrimary(context))
            TextViewCompat.setCompoundDrawableTintList(
                tvLikeCounter, ColorStateList.valueOf(
                    CurrentTheme.getSecondaryTextColorCode(
                        context
                    )
                )
            )
            vAttachmentsRoot = root.findViewById(R.id.item_comment_attachments_root)
            attachmentContainers = forComment((vAttachmentsRoot as ViewGroup))
            animationAdapter = object : WeakViewAnimatorAdapter<View>(selectionView) {
                override fun onAnimationEnd(view: View) {
                    view.visibility = INVISIBLE
                }

                override fun onAnimationStart(view: View) {
                    view.visibility = VISIBLE
                }

                override fun onAnimationCancel(view: View) {
                    view.visibility = INVISIBLE
                }
            }
        }
    }
}