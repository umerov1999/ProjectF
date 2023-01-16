package dev.ragnarok.fenrir.fragment.feedback

import android.app.Activity
import android.graphics.Typeface
import android.text.Spannable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.feedback.FeedbackAdapter.UsersHolder
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.model.feedback.*
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class FeedbackViewBinder(
    private val context: Activity,
    private val attachmentsActionCallback: OnAttachmentsActionCallback
) {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val linkColor: Int = TextView(context).linkTextColors.defaultColor
    private val attachmentsViewBinder: AttachmentsViewBinder =
        AttachmentsViewBinder(context, attachmentsActionCallback)
    private val mLinkActionAdapter: LinkActionAdapter

    /**
     * Настройка отображения уведомления типа "mention_comment_video"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configMentionCommentsVideoFeedback(
        notification: MentionCommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val feedback = notification.where
        val spannable =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.setText(spannable, TextView.BufferType.SPANNABLE)
        var action = AppTextUtils.getDateFromUnixTime(notification.date)
        action = action + SPACE + context.getString(R.string.mentioned_in_comment_photo_video)
        val actionLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.video_dative)
        actionLink.end(action.length)
        val actionSpannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(actionSpannable, actionLink)
        holder.cOwnerTime.setText(actionSpannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        setupAttachmentViewWithVideo(notification.commentOf as Video, holder.ivRightAttachment)
        feedback?.maxAuthorAvaUrl?.let { showUserAvatarOnImageView(it, holder.cOwnerAvatar) }
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    private fun setupAttachmentViewWithVideo(video: Video?, imageView: ImageView) {
        if (video == null || video.image.isNullOrEmpty()) {
            imageView.visibility = View.GONE
        } else {
            imageView.visibility = View.VISIBLE
            with()
                .load(video.image)
                .tag(Constants.PICASSO_TAG)
                .into(imageView)
        }
    }

    private fun setupAttachmentViewWithPhoto(photo: Photo, imageView: ImageView) {
        val photoUrl = photo.getUrlForSize(PhotoSize.X, false)
        if (photoUrl.isNullOrEmpty()) {
            imageView.visibility = View.GONE
        } else {
            imageView.visibility = View.VISIBLE
            with()
                .load(photoUrl)
                .tag(Constants.PICASSO_TAG)
                .into(imageView)
        }
    }

    /**
     * Настройка отображения уведомления типа "mention_comment_photo"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configMentionCommentsPhotoFeedback(
        notification: MentionCommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val feedback = notification.where
        val spannable =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.setText(spannable, TextView.BufferType.SPANNABLE)
        var action = AppTextUtils.getDateFromUnixTime(notification.date)
        action = action + SPACE + context.getString(R.string.mentioned_in_comment_photo_video)
        val actionLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.photo_dative)
        actionLink.end(action.length)
        val actionSpannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(actionSpannable, actionLink)
        holder.cOwnerTime.setText(actionSpannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        with()
            .load(feedback?.maxAuthorAvaUrl)
            .tag(Constants.PICASSO_TAG)
            .transform(transformation)
            .into(holder.cOwnerAvatar)
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        setupAttachmentViewWithPhoto(notification.commentOf as Photo, holder.ivRightAttachment)
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "mention_comments"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configMentionCommentsFeedback(
        notification: MentionCommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val post = notification.commentOf as Post
        val feedback = notification.where
        val spannable =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = true,
                listener = mLinkActionAdapter
            )
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.setText(spannable, TextView.BufferType.SPANNABLE)
        var action = AppTextUtils.getDateFromUnixTime(notification.date)
        action = action + SPACE + context.getString(R.string.in_comments_for_post)
        val actionLink = Link.startOf(action.length)
        val postText = getPostTextCopyIncluded(post)
        action = if (postText.isNullOrEmpty()) {
            action + SPACE + context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(post.date)
            )
        } else {
            action + SPACE + OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(
                postText
            )?.let {
                reduce(
                    it
                )
            }
        }
        actionLink.end(action.length)
        val actionSpannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(actionSpannable, actionLink)
        holder.cOwnerTime.setText(actionSpannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        with()
            .load(feedback?.maxAuthorAvaUrl)
            .tag(Constants.PICASSO_TAG)
            .transform(transformation)
            .into(holder.cOwnerAvatar)
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        holder.ivRightAttachment.visibility = View.GONE
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "mention"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configMentionFeedback(
        notification: MentionFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val feedback = notification.where as Post
        val spannable =
            OwnerLinkSpanFactory.withSpans(
                feedback.text,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        holder.cOwnerName.text = feedback.authorName
        holder.cOwnerText.setText(spannable, TextView.BufferType.SPANNABLE)
        val timeWithActionText =
            AppTextUtils.getDateFromUnixTime(notification.date) + SPACE + context.getString(R.string.mentioned_in_post)
        holder.cOwnerTime.text = timeWithActionText
        holder.cChangable.visibility = View.GONE
        with()
            .load(feedback.authorPhoto)
            .tag(Constants.PICASSO_TAG)
            .transform(transformation)
            .into(holder.cOwnerAvatar)
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(feedback.attachments, containers, true, null, null)
        holder.ivRightAttachment.visibility = View.GONE
        solveOwnerOpenByAvatar(holder.cOwnerAvatar, feedback.authorId)
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "wall_publish"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configWallPublishFeedback(
        notification: PostPublishFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val feedback = notification.post
        val feedBackText =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        var action = AppTextUtils.getDateFromUnixTime(notification.date)
        action = action + SPACE + context.getString(R.string.postings_you_the_news)
        holder.cOwnerName.text = feedback?.authorName
        holder.cOwnerText.setText(feedBackText, TextView.BufferType.SPANNABLE)
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.text = action
        holder.cChangable.visibility = View.GONE
        with()
            .load(feedback?.authorPhoto)
            .tag(Constants.PICASSO_TAG)
            .transform(transformation)
            .into(holder.cOwnerAvatar)
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        holder.ivRightAttachment.visibility = View.GONE
        feedback?.authorId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    private fun solveOwnerOpenByAvatar(ivSource: ImageView, ownerId: Long) {
        ivSource.setOnClickListener { openOwner(ownerId) }
    }

    /**
     * Настройка отображения уведомления типа "wall"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configWallFeedback(
        notification: PostPublishFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val feedback = notification.post
        val feedBackText =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        var action = AppTextUtils.getDateFromUnixTime(notification.date)
        action = action + SPACE + context.getString(R.string.on_your_wall)
        holder.cOwnerName.text = feedback?.authorName
        holder.cOwnerText.setText(feedBackText, TextView.BufferType.SPANNABLE)
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.text = action
        holder.cChangable.visibility = View.GONE
        with()
            .load(feedback?.authorPhoto)
            .tag(Constants.PICASSO_TAG)
            .transform(transformation)
            .into(holder.cOwnerAvatar)
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        holder.ivRightAttachment.visibility = View.GONE
        feedback?.authorId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "comment_video"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configCommentVideoFeedback(
        notification: CommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val feedback = notification.comment
        val feedBackText =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        var action = feedback?.date?.let { AppTextUtils.getDateFromUnixTime(it) }
        action = action + SPACE + context.getString(R.string.comment_your_video_without_video)
        val parentLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.video_accusative)
        parentLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, parentLink)
        val ownername = feedback?.fullAuthorName + SPACE + context.getString(R.string.commented)
        holder.cOwnerName.text = ownername
        holder.cOwnerText.setText(feedBackText, TextView.BufferType.SPANNABLE)
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        feedback?.maxAuthorAvaUrl?.let { showUserAvatarOnImageView(it, holder.cOwnerAvatar) }
        setupAttachmentViewWithVideo(notification.commentOf as Video, holder.ivRightAttachment)
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "comment_photo"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configCommentPhotoFeedback(
        notification: CommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val feedback = notification.comment
        val feedBackText =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        var action = feedback?.date?.let { AppTextUtils.getDateFromUnixTime(it) }
        action = action + SPACE + context.getString(R.string.comment_your_photo_without_photo)
        val parentLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.photo_accusative)
        parentLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, parentLink)
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.setText(feedBackText, TextView.BufferType.SPANNABLE)
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        feedback?.maxAuthorAvaUrl?.let { showUserAvatarOnImageView(it, holder.cOwnerAvatar) }
        setupAttachmentViewWithPhoto(notification.commentOf as Photo, holder.ivRightAttachment)
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "comment_post"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configCommentPostFeedback(
        notification: CommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val post = notification.commentOf as Post
        val feedback = notification.comment
        val feedBackText = OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(feedback?.text)
        var action = feedback?.date?.let { AppTextUtils.getDateFromUnixTime(it) }
        action = action + SPACE + context.getString(R.string.for_your_post)
        val parentLink = Link.startOf(action.length)
        val parentText =
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(getPostTextCopyIncluded(post))
        action = if (parentText.isNullOrEmpty()) {
            action + SPACE + context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(post.date)
            )
        } else {
            action + SPACE + parentText
        }
        parentLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, parentLink)
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.text = feedBackText
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        val postImage = post.findFirstImageCopiesInclude()
        if (postImage.isNullOrEmpty()) {
            holder.ivRightAttachment.visibility = View.GONE
        } else {
            holder.ivRightAttachment.visibility = View.VISIBLE
            with()
                .load(postImage)
                .tag(Constants.PICASSO_TAG)
                .into(holder.ivRightAttachment)
        }
        feedback?.maxAuthorAvaUrl?.let { showUserAvatarOnImageView(it, holder.cOwnerAvatar) }
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "reply_comment_video"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configReplyCommentVideoFeedback(
        notification: ReplyCommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val parent = notification.ownComment
        val feedback = notification.feedbackComment
        val feedBackText = OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(feedback?.text)
        var action = AppTextUtils.getDateFromUnixTime(feedback?.date.orZero())
        action = action + SPACE + context.getString(R.string.in_reply_to_your_comment)
        val parentText: String = if (parent?.text.isNullOrEmpty()) {
            context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(parent?.date.orZero())
            )
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(parent?.text)?.let { reduce(it) }
                ?: ""
        }
        val parentLink = Link.startOf(action.length)
        action = action + SPACE + parentText
        parentLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, parentLink)
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.text = feedBackText
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        setupAttachmentViewWithVideo(notification.commentsOf as Video, holder.ivRightAttachment)
        feedback?.maxAuthorAvaUrl?.let { showUserAvatarOnImageView(it, holder.cOwnerAvatar) }
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "reply_comment_photo"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configReplyCommentPhotoFeedback(
        notification: ReplyCommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val parent = notification.ownComment
        val feedback = notification.feedbackComment
        setupAttachmentViewWithPhoto(notification.commentsOf as Photo, holder.ivRightAttachment)
        val feedBackText = OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(feedback?.text)
        var action = AppTextUtils.getDateFromUnixTime(feedback?.date.orZero())
        action = action + SPACE + context.getString(R.string.in_reply_to_your_comment)
        val parentText: String = if (parent?.text.isNullOrEmpty()) {
            context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(parent?.date.orZero())
            )
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(parent?.text)?.let { reduce(it) }
                ?: ""
        }
        val parentLink = Link.startOf(action.length)
        action = action + SPACE + parentText
        parentLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, parentLink)
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.text = feedBackText
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        feedback?.maxAuthorAvaUrl?.let { showUserAvatarOnImageView(it, holder.cOwnerAvatar) }
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "reply_topic"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configReplyTopicFeedback(
        notification: ReplyCommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val topic = notification.commentsOf as Topic
        val feedback = notification.feedbackComment
        val feedBackText =
            OwnerLinkSpanFactory.withSpans(
                feedback?.text,
                owners = true,
                topics = true,
                listener = mLinkActionAdapter
            )
        holder.cOwnerText.text = feedBackText
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        var action = AppTextUtils.getDateFromUnixTime(feedback?.date.orZero())
        action = action + SPACE + context.getString(R.string.in_reply_to_your_message_in_topic)
        val parentLink = Link.startOf(action.length)
        action = action + SPACE + topic.title
        parentLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, parentLink)
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerTime.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        with()
            .load(feedback?.maxAuthorAvaUrl)
            .tag(Constants.PICASSO_TAG)
            .transform(transformation)
            .into(holder.cOwnerAvatar)
        holder.ivRightAttachment.visibility = View.GONE
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "reply_comment"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configReplyCommentFeedback(
        notification: ReplyCommentFeedback,
        holder: FeedbackAdapter.CommentHolder
    ) {
        val parent = notification.ownComment
        val feedback = notification.feedbackComment
        val feedBackText = OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(feedback?.text)
        var action = AppTextUtils.getDateFromUnixTime(notification.date)
        action = action + SPACE + context.getString(R.string.in_reply_to_your_comment)
        val parentText: String = if (parent?.text.isNullOrEmpty()) {
            context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(parent?.date.orZero())
            )
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(parent?.text)?.let { reduce(it) }
                ?: ""
        }
        val parentLink = Link.startOf(action.length)
        action = action + SPACE + parentText
        parentLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, parentLink)
        holder.cOwnerName.text = feedback?.fullAuthorName
        holder.cOwnerText.text = feedBackText
        holder.cOwnerText.visibility =
            if (feedBackText.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.cOwnerTime.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.cChangable.visibility = View.GONE
        with()
            .load(feedback?.maxAuthorAvaUrl)
            .tag(Constants.PICASSO_TAG)
            .transform(transformation)
            .into(holder.cOwnerAvatar)
        val containers = AttachmentsHolder.forFeedback(holder.vAttachmentsRoot)
        attachmentsViewBinder.displayAttachments(
            feedback?.attachments,
            containers,
            true,
            null,
            null
        )
        holder.ivRightAttachment.visibility = View.GONE
        feedback?.fromId?.let { solveOwnerOpenByAvatar(holder.cOwnerAvatar, it) }
        configReply(notification.reply, holder)
    }

    /**
     * Настройка отображения уведомления типа "follow"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configFollowFeedback(notification: UsersFeedback, holder: UsersHolder) {
        val users = notification.owners
        val user = users?.get(0)
        var action = genFullUsersString(users.orEmpty())
        action = action + SPACE + context.getString(R.string.subscribed_to_your_updates)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        showFirstUserAvatarOnImageView(notification.owners, holder.uAvatar)
        holder.uInfo.visibility = View.GONE
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.plus)
        holder.ivAttachment.visibility = View.GONE
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    private fun configReply(reply: Comment?, holder: FeedbackAdapter.CommentHolder) {
        holder.cReplyContainer.visibility = if (reply == null) View.GONE else View.VISIBLE
        if (reply != null) {
            holder.cReplyName.text = reply.fullAuthorName
            holder.cReplyTime.text = AppTextUtils.getDateFromUnixTime(reply.date)
            holder.cReplyText.text = OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(reply.text)
            val authorAvaUrl = reply.maxAuthorAvaUrl
            displayAvatar(
                holder.cReplyOwnerAvatar,
                transformation,
                authorAvaUrl,
                Constants.PICASSO_TAG
            )
            val replyContainers = AttachmentsHolder.forFeedback(holder.vReplyAttachmentsRoot)
            attachmentsViewBinder.displayAttachments(
                reply.attachments,
                replyContainers,
                true,
                null,
                null
            )
            holder.cReplyOwnerAvatar.setOnClickListener { openOwner(reply.fromId) }
        }
    }

    /**
     * Настройка отображения уведомления типа "friend_accepted"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configFriendAcceptedFeedback(notification: UsersFeedback, holder: UsersHolder) {
        val owners = notification.owners
        val user = owners?.get(0)
        var action = genFullUsersString(owners.orEmpty())
        action = action + SPACE + context.getString(R.string.accepted_friend_request)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        showFirstUserAvatarOnImageView(owners, holder.uAvatar)
        holder.uInfo.visibility = View.GONE
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.plus)
        holder.ivAttachment.visibility = View.GONE
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "like_post"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configLikePostFeedback(notification: LikeFeedback, holder: UsersHolder) {
        val post = notification.liked as Post
        val owners = notification.owners
        val user = owners?.get(0)
        var action = genFullUsersString(owners.orEmpty())
        action = action + SPACE + context.getString(R.string.liked_your_post)
        val info: String
        val postTitle = getPostTextCopyIncluded(post)
        info = if (postTitle.isNullOrEmpty()) {
            context.getString(R.string.from_date, AppTextUtils.getDateFromUnixTime(post.date))
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(postTitle)?.let { reduce(it) } ?: ""
        }
        val postLink = Link.startOf(0)
        postLink.end(info.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(info)
        showAsLink(spannable, postLink)
        holder.uName.text = action
        holder.uInfo.visibility = View.VISIBLE
        holder.uInfo.setText(spannable, TextView.BufferType.SPANNABLE)
        showFirstUserAvatarOnImageView(owners, holder.uAvatar)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.heart_filled)
        val attachmentImg = post.findFirstImageCopiesInclude()
        holder.ivAttachment.visibility =
            if (attachmentImg.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (attachmentImg.nonNullNoEmpty()) {
            with().load(attachmentImg).into(holder.ivAttachment)
        }
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "like_video"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configLikeVideoFeedback(notification: LikeFeedback, holder: UsersHolder) {
        val users = notification.owners
        val user = users?.get(0)
        var action = users?.let { genFullUsersString(it) }
        action = action + SPACE + context.getString(R.string.liked_your_video_without_video)
        val photoLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.video_accusative)
        photoLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, photoLink)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uInfo.visibility = View.GONE
        holder.uChangable.setIcon(R.drawable.heart_filled)
        holder.ivAttachment.visibility = View.VISIBLE
        showFirstUserAvatarOnImageView(notification.owners, holder.uAvatar)
        setupAttachmentViewWithVideo(notification.liked as Video, holder.ivAttachment)
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "copy_photo"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configCopyPhotoFeedback(notification: CopyFeedback, holder: UsersHolder) {
        val users = notification.owners
        val user = users?.get(0)
        var action = genFullUsersString(users.orEmpty())
        action = action + SPACE + context.getString(R.string.copy_your_photo_without_photo)
        val link = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.photo_ablative)
        link.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, link)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uInfo.visibility = View.GONE
        holder.uChangable.setIcon(R.drawable.share)
        showFirstUserAvatarOnImageView(notification.owners, holder.uAvatar)
        setupAttachmentViewWithPhoto(notification.what as Photo, holder.ivAttachment)
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "copy_video"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configCopyVideoFeedback(notification: CopyFeedback, holder: UsersHolder) {
        val users = notification.owners
        val user = users?.get(0)
        var action = genFullUsersString(users.orEmpty())
        action = action + SPACE + context.getString(R.string.copy_your_video_without_video)
        val link = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.video_ablative)
        link.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, link)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.uInfo.visibility = View.GONE
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.share)
        showFirstUserAvatarOnImageView(notification.owners, holder.uAvatar)
        setupAttachmentViewWithVideo(notification.what as Video, holder.ivAttachment)
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "copy_post"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configCopyPostFeedback(notification: CopyFeedback, holder: UsersHolder) {
        val post = notification.what as Post
        val users = notification.owners
        var action = genFullUsersString(users.orEmpty())
        action = action + SPACE + context.getString(R.string.shared_post)
        val link = Link.startOf(0)
        val postText = getPostTextCopyIncluded(post)
        val info: String = if (postText.isNullOrEmpty()) {
            context.getString(R.string.from_date, AppTextUtils.getDateFromUnixTime(post.date))
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(postText)?.let { reduce(it) } ?: ""
        }
        link.end(info.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(info)
        showAsLink(spannable, link)
        holder.uName.text = action
        holder.uInfo.visibility = View.VISIBLE
        holder.uInfo.setText(spannable, TextView.BufferType.SPANNABLE)
        showFirstUserAvatarOnImageView(notification.owners, holder.uAvatar)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.share)
        val firstAttachmentUrl = post.findFirstImageCopiesInclude()
        holder.ivAttachment.visibility =
            if (firstAttachmentUrl.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (firstAttachmentUrl.nonNullNoEmpty()) {
            with()
                .load(firstAttachmentUrl)
                .tag(Constants.PICASSO_TAG)
                .into(holder.ivAttachment)
        }
        users?.get(0)?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "like_photo"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configLikePhotoFeedback(notification: LikeFeedback, holder: UsersHolder) {
        val owners = notification.owners
        val firstUser = owners?.get(0)
        var action = genFullUsersString(owners.orEmpty())
        action = action + SPACE + context.getString(R.string.liked_your_photo_without_photo)
        val photoLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.photo_accusative)
        photoLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        showAsLink(spannable, photoLink)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.uInfo.visibility = View.GONE
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.heart_filled)
        setupAttachmentViewWithPhoto(notification.liked as Photo, holder.ivAttachment)
        showFirstUserAvatarOnImageView(owners, holder.uAvatar)
        firstUser?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Позволяет получить к примеру "Евгений Румянцев и ещё 6 человек оценили Вашу фотографию"
     * для заглавия ответа
     *
     * @param users массив пользователей, которые "ответили"
     * @return строка
     */
    private fun genFullUsersString(users: List<Owner>): String {
        val owner = users[0]
        var action = owner.fullName ?: "null"
        if (users.size > 1) {
            action =
                action + SPACE + context.getString(R.string.and_users_count_more, users.size - 1)
        }
        return action
    }

    /**
     * Настройка отображения уведомления типа "like_comment_photo"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configLikeCommentForPhotoFeedback(notification: LikeCommentFeedback, holder: UsersHolder) {
        val comment = notification.liked
        val users = notification.owners
        val user = users?.get(0)
        var action = user?.fullName
        if (users?.size.orZero() > 1) {
            action =
                action + SPACE + context.getString(
                    R.string.and_users_count_more,
                    users?.size.orZero() - 1
                )
        }
        action = action + SPACE + context.getString(R.string.liked_comment)
        val commentText: String = if (comment?.text.isNullOrEmpty()) {
            context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(comment?.date.orZero())
            )
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(comment?.text)?.let { reduce(it) }
                ?: ""
        }
        val commentLink = Link.startOf(0)
        commentLink.end(commentText.length)
        action = action + SPACE + context.getString(R.string.keyword_for)
        val photoLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.photo_dative)
        photoLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        val commentSpan = Spannable.Factory.getInstance().newSpannable(commentText)
        showAsLink(commentSpan, commentLink)
        showAsLink(spannable, photoLink)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.uInfo.visibility = View.VISIBLE
        holder.uInfo.setText(commentSpan, TextView.BufferType.SPANNABLE)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.heart_filled)
        setupAttachmentViewWithPhoto(notification.commented as Photo, holder.ivAttachment)
        showFirstUserAvatarOnImageView(users, holder.uAvatar)
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "like_comment_video"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configLikeCommentVideoFeedback(notification: LikeCommentFeedback, holder: UsersHolder) {
        val comment = notification.liked
        val users = notification.owners
        val user = users?.get(0)
        var action = user?.fullName
        if (users?.size.orZero() > 1) {
            action =
                action + SPACE + context.getString(
                    R.string.and_users_count_more,
                    users?.size.orZero() - 1
                )
        }
        action = action + SPACE + context.getString(R.string.liked_comment)
        val commentText: String = if (comment?.text.isNullOrEmpty()) {
            context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(comment?.date.orZero())
            )
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(comment?.text)?.let { reduce(it) }
                ?: ""
        }
        val commentLink = Link.startOf(0)
        commentLink.end(commentText.length)
        action = action + SPACE + context.getString(R.string.keyword_for)
        val photoLink = Link.startOf(action.length)
        action = action + SPACE + context.getString(R.string.video_dative)
        photoLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        val spannable1 = Spannable.Factory.getInstance().newSpannable(commentText)
        showAsLink(spannable1, commentLink)
        showAsLink(spannable, photoLink)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.uInfo.visibility = View.VISIBLE
        holder.uInfo.setText(spannable1, TextView.BufferType.SPANNABLE)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.heart_filled)
        showFirstUserAvatarOnImageView(users, holder.uAvatar)
        setupAttachmentViewWithVideo(notification.commented as Video, holder.ivAttachment)
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Настройка отображения уведомления типа "like_comment_topic"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configLikeCommentTopicFeedback(notification: LikeCommentFeedback, holder: UsersHolder) {
        val comment = notification.liked
        val users = notification.owners
        val user = users?.get(0)
        val topic = notification.commented as Topic
        holder.ivAttachment.visibility = View.GONE
        var action = user?.fullName
        if (users?.size.orZero() > 1) {
            action =
                action + SPACE + context.getString(
                    R.string.and_users_count_more,
                    users?.size.orZero() - 1
                )
        }
        action = action + SPACE + context.getString(R.string.liked_comment)
        val commentText: String = if (comment?.text.isNullOrEmpty()) {
            context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(comment?.date.orZero())
            )
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(comment?.text)?.let { reduce(it) }
                ?: ""
        }
        val commentLink = Link.startOf(0)
        commentLink.end(commentText.length)
        action = action + SPACE + context.getString(R.string.in_topic)
        val photoLink = Link.startOf(action.length)
        action = action + SPACE + topic.title
        photoLink.end(action.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(action)
        val spannable1 = Spannable.Factory.getInstance().newSpannable(commentText)
        showAsLink(spannable1, commentLink)
        showAsLink(spannable, photoLink)
        holder.uName.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.uInfo.visibility = View.VISIBLE
        holder.uInfo.setText(spannable1, TextView.BufferType.SPANNABLE)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.heart_filled)
        showFirstUserAvatarOnImageView(users, holder.uAvatar)
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    private fun showAsLink(spannable: Spannable, link: Link) {
        spannable.setSpan(
            ForegroundColorSpan(linkColor),
            link.start,
            link.end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            StyleSpan(Typeface.ITALIC),
            link.start,
            link.end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    /**
     * Настройка отображения уведомления типа "like_comment"
     *
     * @param notification уведомление
     * @param holder       контейнер в елементами интерфейса
     */
    fun configLikeCommentFeedback(notification: LikeCommentFeedback, holder: UsersHolder) {
        val comment = notification.liked
        val users = notification.owners
        val post = notification.commented as Post
        val user = users?.get(0)
        var action = user?.fullName
        if (users?.size.orZero() > 1) {
            action =
                action + SPACE + context.getString(
                    R.string.and_users_count_more,
                    users?.size.orZero() - 1
                )
        }
        action = action + SPACE + context.getString(R.string.liked_comment)
        val commentText: String = if (comment?.text.isNullOrEmpty()) {
            context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(comment?.date.orZero())
            )
        } else {
            OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(comment?.text)?.let { reduce(it) }
                ?: ""
        }
        val commentLink = Link.startOf(0)
        commentLink.end(commentText.length)
        var info = commentText
        info = info + SPACE + context.getString(R.string.for_post)
        val postLink = Link.startOf(info.length)
        info = if (post.text.isNullOrEmpty()) {
            info + SPACE + context.getString(
                R.string.from_date,
                AppTextUtils.getDateFromUnixTime(post.date)
            )
        } else {
            info + SPACE + OwnerLinkSpanFactory.getTextWithCollapseOwnerLinks(
                post.text
            )?.let {
                reduce(
                    it
                )
            }
        }
        postLink.end(info.length)
        val spannable = Spannable.Factory.getInstance().newSpannable(info)
        showAsLink(spannable, commentLink)
        showAsLink(spannable, postLink)
        holder.uName.text = action
        holder.uInfo.visibility = View.VISIBLE
        holder.uInfo.setText(info, TextView.BufferType.SPANNABLE)
        holder.uTime.text = AppTextUtils.getDateFromUnixTime(notification.date)
        holder.uChangable.setIcon(R.drawable.heart_filled)
        holder.ivAttachment.visibility = View.GONE
        showFirstUserAvatarOnImageView(users, holder.uAvatar)
        user?.ownerId?.let { solveOwnerOpenByAvatar(holder.uAvatar, it) }
    }

    /**
     * Обрезать строку до 100 символов
     *
     * @param input строка, которую надо обрезать
     * @return обрезанная строка
     */
    private fun reduce(input: String): String {
        return if (input.length > 100) input.substring(0, 100) + "..." else input
    }

    /**
     * Отобразить аватар пользователя на ImageView
     *
     * @param url       сслыка на аватра
     * @param imageView вьюв
     */
    private fun showUserAvatarOnImageView(url: String, imageView: ImageView) {
        displayAvatar(imageView, transformation, url, Constants.PICASSO_TAG)
    }

    /**
     * Отображение аватара первого в списке пользователя на ImageView.
     * Если у пользователя нет аватара, то будет отображено изображение
     * неизвестного пользователя
     *
     * @param owners    массив пользователей
     * @param imageView вьюв
     */
    private fun showFirstUserAvatarOnImageView(owners: List<Owner>?, imageView: ImageView) {
        if (owners.isNullOrEmpty() || owners[0].maxSquareAvatar.isNullOrEmpty()) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .tag(Constants.PICASSO_TAG)
                .into(imageView)
        } else {
            val url = owners[0].maxSquareAvatar
            with()
                .load(url)
                .tag(Constants.PICASSO_TAG)
                .transform(transformation)
                .into(imageView)
        }
    }

    /**
     * Получить заглавие поста.
     * Если у поста нет заглавия, то проверяеться история репостов,
     * возвращается первое найденное заглавие
     *
     * @param post пост
     * @return заглавие
     */
    private fun getPostTextCopyIncluded(post: Post): String? {
        return if (post.text.isNullOrEmpty() && post.hasCopyHierarchy()) {
            for (copy in post.getCopyHierarchy().orEmpty()) {
                if (copy.hasText()) {
                    return copy.text
                }
            }
            null
        } else {
            post.text
        }
    }

    internal fun openOwner(userId: Long) {
        attachmentsActionCallback.onOpenOwner(userId)
    }

    private class Link private constructor(val start: Int) {
        var end = 0
        fun end(end: Int) {
            this.end = end
        }

        companion object {
            fun startOf(start: Int): Link {
                return Link(start)
            }
        }
    }

    companion object {
        private const val SPACE = " "
    }

    init {
        mLinkActionAdapter = object : LinkActionAdapter() {
            override fun onOwnerClick(ownerId: Long) {
                openOwner(ownerId)
            }
        }
    }
}