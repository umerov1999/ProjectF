package dev.ragnarok.fenrir.fragment.newsfeedcomments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.*
import dev.ragnarok.fenrir.fragment.base.AbsRecyclerViewAdapter
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder.Companion.forComment
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder.Companion.forPost
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.newsfeedcomments.NewsfeedCommentsAdapter.AbsHolder
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.*
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.AspectRatioImageView
import dev.ragnarok.fenrir.view.VideoServiceIcons.getIconByType
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView

class NewsfeedCommentsAdapter(
    private val context: Context, private var data: List<NewsfeedComment>,
    callback: OnAttachmentsActionCallback
) : AbsRecyclerViewAdapter<AbsHolder>() {
    private val attachmentsViewBinder: AttachmentsViewBinder =
        AttachmentsViewBinder(context, callback)
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val linkActionAdapter: LinkActionAdapter
    private val colorTextSecondary: Int = CurrentTheme.getSecondaryTextColorCode(context)
    private val iconColorActive: Int = CurrentTheme.getColorPrimary(context)
    private var actionListener: ActionListener? = null
    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbsHolder {
        val inflater = LayoutInflater.from(parent.context)
        when (viewType) {
            VTYPE_POST -> return PostHolder(
                inflater.inflate(
                    R.layout.item_newsfeed_comment_post,
                    parent,
                    false
                )
            )

            VTYPE_VIDEO -> return VideoHolder(
                inflater.inflate(
                    R.layout.item_newsfeed_comment_video,
                    parent,
                    false
                )
            )

            VTYPE_PHOTO -> return PhotoHolder(
                inflater.inflate(
                    R.layout.item_newsfeed_comment_photo,
                    parent,
                    false
                )
            )

            VTYPE_TOPIC -> return TopicHolder(
                inflater.inflate(
                    R.layout.item_newsfeed_comment_topic,
                    parent,
                    false
                )
            )
        }
        throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: AbsHolder, position: Int) {
        bindBase(holder, position)
        when (getItemViewType(position)) {
            VTYPE_POST -> bindPost(holder as PostHolder, position)
            VTYPE_VIDEO -> bindVideo(holder as VideoHolder, position)
            VTYPE_PHOTO -> bindPhoto(holder as PhotoHolder, position)
            VTYPE_TOPIC -> bindTopic(holder as TopicHolder, position)
        }
    }

    private fun bindTopic(holder: TopicHolder, position: Int) {
        val wrapper = data[position].getModel() as TopicWithOwner
        val topic = wrapper.getTopic()
        val owner = wrapper.getOwner()
        displayAvatar(
            holder.ownerAvatar,
            transformation,
            owner.maxSquareAvatar,
            Constants.PICASSO_TAG
        )
        topic.creator.ifNonNull({
            holder.creatorAvatar.visibility = View.VISIBLE
            displayAvatar(
                holder.creatorAvatar,
                transformation,
                it.get100photoOrSmaller(),
                Constants.PICASSO_TAG
            )
        }, {
            holder.creatorAvatar.visibility = View.GONE
            with().cancelRequest(holder.creatorAvatar)
        })
        addOwnerAvatarClickHandling(holder.ownerAvatar, topic.ownerId)
        holder.ownerName.text = owner.fullName
        holder.commentsCounter.text = topic.commentsCount.toString()
        holder.title.text = topic.title
    }

    private fun bindPhoto(holder: PhotoHolder, position: Int) {
        val wrapper = data[position].getModel() as PhotoWithOwner
        val photo = wrapper.getPhoto()
        val owner = wrapper.getOwner()
        displayAvatar(
            holder.ownerAvatar,
            transformation,
            owner.maxSquareAvatar,
            Constants.PICASSO_TAG
        )
        addOwnerAvatarClickHandling(holder.ownerAvatar, photo.ownerId)
        holder.ownerName.text = owner.fullName
        holder.dateTime.text = AppTextUtils.getDateFromUnixTime(context, photo.date)
        holder.title.visibility =
            if (photo.text.nonNullNoEmpty()) View.VISIBLE else View.GONE
        holder.title.text = photo.text
        if (photo.width > photo.height) {
            holder.image.setAspectRatio(photo.width, photo.height)
            holder.image.scaleType = ImageView.ScaleType.CENTER_CROP
        } else {
            holder.image.setAspectRatio(1, 1)
            holder.image.scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val photoUrl = photo.getUrlForSize(PhotoSize.X, true)
        if (photoUrl.nonNullNoEmpty()) {
            with()
                .load(photoUrl)
                .into(holder.image)
        }
    }

    private fun bindVideo(holder: VideoHolder, position: Int) {
        val wrapper = data[position].getModel() as VideoWithOwner
        val video = wrapper.getVideo()
        val owner = wrapper.getOwner()
        holder.title.text = video.title
        holder.viewsCounter.text = video.views.toString()
        holder.datitime.text = AppTextUtils.getDateFromUnixTime(context, video.date)
        val serviceIcon = getIconByType(video.platform)
        if (serviceIcon != null) {
            holder.service.visibility = View.VISIBLE
            holder.service.setImageResource(serviceIcon)
        } else {
            holder.service.visibility = View.GONE
        }
        if (video.image.nonNullNoEmpty()) {
            with()
                .load(video.image)
                .into(holder.image)
        } else {
            with()
                .cancelRequest(holder.image)
        }
        holder.duration.text = AppTextUtils.getDurationString(video.duration)
        holder.ownerName.text = owner.fullName
        displayAvatar(holder.avatar, transformation, owner.maxSquareAvatar, Constants.PICASSO_TAG)
        addOwnerAvatarClickHandling(holder.avatar, video.ownerId)
    }

    private fun bindBase(holder: AbsHolder, position: Int) {
        val newsfeedComment = data[position]
        val comment = newsfeedComment.getComment()
        if (comment == null) {
            holder.commentRoot.visibility = View.GONE
            return
        }
        holder.commentRoot.visibility = View.VISIBLE
        holder.commentRoot.setOnClickListener {
            actionListener?.onCommentBodyClick(newsfeedComment)
        }
        holder.commentAttachmentRoot.visibility =
            if (comment.hasAttachments()) View.VISIBLE else View.GONE
        attachmentsViewBinder.displayAttachments(
            comment.attachments,
            holder.commentAttachmentHolder,
            true,
            null,
            null
        )
        displayAvatar(
            holder.commentAvatar,
            transformation,
            comment.maxAuthorAvaUrl,
            Constants.PICASSO_TAG
        )
        holder.commentAuthorName.text = comment.fullAuthorName
        holder.commentDatetime.text = AppTextUtils.getDateFromUnixTime(context, comment.date)
        val text = OwnerLinkSpanFactory.withSpans(
            comment.text,
            owners = true,
            topics = true,
            listener = linkActionAdapter
        )
        holder.commentText.setText(text, TextView.BufferType.SPANNABLE)
        holder.commentText.visibility =
            if (comment.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.commentLikeCounter.visibility =
            if (comment.likesCount > 0) View.VISIBLE else View.GONE
        holder.commentLikeCounter.text = comment.likesCount.toString()
        TextViewCompat.setCompoundDrawableTintList(
            holder.commentLikeCounter,
            ColorStateList.valueOf(if (comment.isUserLikes) iconColorActive else colorTextSecondary)
        )
        if (comment.fromId != 0L) {
            addOwnerAvatarClickHandling(holder.commentAvatar, comment.fromId)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindPost(holder: PostHolder, position: Int) {
        val comment = data[position]
        val post = comment.getModel() as Post
        attachmentsViewBinder.displayAttachments(
            post.attachments,
            holder.postAttachmentsHolder,
            false,
            null,
            null
        )
        attachmentsViewBinder.displayCopyHistory(
            post.getCopyHierarchy(),
            holder.postAttachmentsHolder.vgPosts,
            true,
            R.layout.item_copy_history_post
        )
        holder.ownerName.text = post.authorName
        holder.postDatetime.text = AppTextUtils.getDateFromUnixTime(context, post.date)
        displayAvatar(holder.ownerAvatar, transformation, post.authorPhoto, Constants.PICASSO_TAG)
        addOwnerAvatarClickHandling(holder.ownerAvatar, post.ownerId)
        val reduced = AppTextUtils.reduceStringForPost(post.text)
        holder.postText.text =
            OwnerLinkSpanFactory.withSpans(
                reduced,
                owners = true,
                topics = false,
                listener = linkActionAdapter
            )
        holder.buttonShowMore.visibility =
            if (post.hasText() && post.text?.length.orZero() > 400) View.VISIBLE else View.GONE
        holder.postTextRoot.visibility = if (post.hasText()) View.VISIBLE else View.GONE
        holder.signerRoot.visibility = if (post.creator == null) View.GONE else View.VISIBLE
        addOwnerAvatarClickHandling(holder.signerRoot, post.signerId)
        post.creator.requireNonNull {
            holder.signerName.text = it.fullName
            displayAvatar(
                holder.signerAvatar,
                transformation,
                it.photo50,
                Constants.PICASSO_TAG
            )
        }
        holder.viewsCounter.text = post.viewCount.toString()
        holder.viewsCounter.visibility = if (post.viewCount > 0) View.VISIBLE else View.GONE
        holder.friendsOnlyIcon.visibility = if (post.isFriendsOnly) View.VISIBLE else View.GONE
        holder.topDivider.visibility =
            if (needToShowTopDivider(post)) View.VISIBLE else View.GONE
        val postOpenClickListener = View.OnClickListener {
            actionListener?.onPostBodyClick(comment)
        }
        holder.postRoot.setOnClickListener(postOpenClickListener)
        post.copyright?.let { vit ->
            holder.tvCopyright.visibility = View.VISIBLE
            holder.tvCopyright.text = "©" + vit.name
            holder.tvCopyright.setOnClickListener {
                LinkHelper.openUrl(
                    holder.tvCopyright.context as Activity,
                    Settings.get().accounts().current,
                    vit.link,
                    false
                )
            }
        } ?: run { holder.tvCopyright.visibility = View.GONE }
    }

    private fun needToShowTopDivider(post: Post): Boolean {
        if (post.text.nonNullNoEmpty()) {
            return true
        }
        val attachments = post.attachments
        // если есть копи-хистори и нет вложений фото-видео в главном посте
        if (post.getCopyHierarchy()
                .nonNullNoEmpty() && (attachments == null || (attachments.photos.safeAllIsNullOrEmpty() && attachments.videos.safeAllIsNullOrEmpty()))
        ) {
            return true
        }
        return if (post.attachments == null) {
            true
        } else attachments?.photos.safeAllIsNullOrEmpty() && attachments?.videos.safeAllIsNullOrEmpty()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val comment = data[position]
        if (comment.getModel() is Post) {
            return VTYPE_POST
        }
        if (comment.getModel() is VideoWithOwner) {
            return VTYPE_VIDEO
        }
        if (comment.getModel() is PhotoWithOwner) {
            return VTYPE_PHOTO
        }
        if (comment.getModel() is TopicWithOwner) {
            return VTYPE_TOPIC
        }
        throw IllegalStateException("Unsupported view type")
    }

    fun setData(data: List<NewsfeedComment>) {
        this.data = data
        notifyDataSetChanged()
    }

    interface ActionListener {
        fun onPostBodyClick(comment: NewsfeedComment)
        fun onCommentBodyClick(comment: NewsfeedComment)
    }

    private class TopicHolder(itemView: View) : AbsHolder(itemView) {
        val ownerAvatar: ImageView = itemView.findViewById(R.id.owner_avatar)
        val creatorAvatar: ImageView = itemView.findViewById(R.id.creator_avatar)
        val commentsCounter: TextView = itemView.findViewById(R.id.comments_counter)
        val ownerName: TextView = itemView.findViewById(R.id.owner_name)
        val title: TextView = itemView.findViewById(R.id.title)

    }

    abstract class AbsHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val commentRoot: View = itemView.findViewById(R.id.comment_root)
        val commentAvatar: ImageView = itemView.findViewById(R.id.item_comment_owner_avatar)
        val commentAuthorName: TextView = itemView.findViewById(R.id.item_comment_owner_name)
        val commentText: EmojiconTextView = itemView.findViewById(R.id.item_comment_text)
        val commentDatetime: TextView = itemView.findViewById(R.id.item_comment_time)
        val commentLikeCounter: TextView = itemView.findViewById(R.id.item_comment_like_counter)
        val commentAttachmentRoot: ViewGroup =
            commentRoot.findViewById(R.id.item_comment_attachments_root)
        val commentAttachmentHolder: AttachmentsHolder = forComment(commentAttachmentRoot)

    }

    private class PhotoHolder(itemView: View) : AbsHolder(itemView) {
        val ownerAvatar: ImageView = itemView.findViewById(R.id.photo_owner_avatar)
        val ownerName: TextView = itemView.findViewById(R.id.photo_owner_name)
        val dateTime: TextView = itemView.findViewById(R.id.photo_datetime)
        val title: TextView = itemView.findViewById(R.id.photo_title)
        val image: AspectRatioImageView = itemView.findViewById(R.id.photo_image)
    }

    private class VideoHolder(itemView: View) : AbsHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.video_title)
        val datitime: TextView = itemView.findViewById(R.id.video_datetime)
        val viewsCounter: TextView = itemView.findViewById(R.id.video_views_counter)
        val service: ImageView = itemView.findViewById(R.id.video_service)
        val image: ImageView = itemView.findViewById(R.id.video_image)
        val duration: TextView = itemView.findViewById(R.id.video_lenght)
        val avatar: ImageView = itemView.findViewById(R.id.video_owner_avatar)
        val ownerName: TextView = itemView.findViewById(R.id.video_owner_name)
    }

    private class PostHolder(itemView: View) : AbsHolder(itemView) {
        val ownerAvatar: ImageView = itemView.findViewById(R.id.item_post_avatar)
        val ownerName: TextView = itemView.findViewById(R.id.item_post_owner_name)
        val postDatetime: TextView = itemView.findViewById(R.id.item_post_time)
        val postTextRoot: View = itemView.findViewById(R.id.item_text_container)
        val postText: EmojiconTextView = itemView.findViewById(R.id.item_post_text)
        val buttonShowMore: View = itemView.findViewById(R.id.item_post_show_more)
        val signerRoot: View
        val signerAvatar: ImageView
        val signerName: TextView
        val postAttachmentsHolder: AttachmentsHolder
        val viewsCounter: TextView
        val friendsOnlyIcon: View
        val topDivider: View = itemView.findViewById(R.id.top_divider)
        val postRoot: View
        val tvCopyright: TextView

        init {
            val postAttachmentRoot = itemView.findViewById<ViewGroup>(R.id.item_post_attachments)
            postAttachmentsHolder = forPost(postAttachmentRoot)
            signerRoot = itemView.findViewById(R.id.item_post_signer_root)
            signerAvatar = itemView.findViewById(R.id.item_post_signer_icon)
            signerName = itemView.findViewById(R.id.item_post_signer_name)
            viewsCounter = itemView.findViewById(R.id.post_views_counter)
            friendsOnlyIcon = itemView.findViewById(R.id.item_post_friends_only)
            tvCopyright = itemView.findViewById(R.id.item_post_copyright)
            postRoot = itemView.findViewById(R.id.post_root)
        }
    }

    companion object {
        private const val VTYPE_POST = 1
        private const val VTYPE_VIDEO = 2
        private const val VTYPE_PHOTO = 3
        private const val VTYPE_TOPIC = 4
    }

    init {
        linkActionAdapter = object : LinkActionAdapter() { // do nothing
        }
    }
}