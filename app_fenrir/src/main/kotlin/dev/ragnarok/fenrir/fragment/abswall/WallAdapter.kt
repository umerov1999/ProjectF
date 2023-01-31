package dev.ragnarok.fenrir.fragment.abswall

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.api.model.VKApiPostSource
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder.Companion.forPost
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.Post
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView

class WallAdapter(
    private val mContext: Context,
    items: MutableList<Post>,
    attachmentsActionCallback: OnAttachmentsActionCallback,
    adapterListener: ClickListener
) : RecyclerBindableAdapter<Post, RecyclerView.ViewHolder>(items) {
    private val attachmentsViewBinder: AttachmentsViewBinder =
        AttachmentsViewBinder(mContext, attachmentsActionCallback)
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val clickListener: ClickListener = adapterListener
    private val mLinkActionAdapter: LinkActionAdapter
    private var nonPublishedPostActionListener: NonPublishedPostActionListener? = null
    private var mOnHashTagClickListener: EmojiconTextView.OnHashTagClickListener? = null
    override fun onBindItemViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int,
        type: Int
    ) {
        val item = getItem(position)
        when (type) {
            TYPE_NORMAL -> {
                val normalHolder = viewHolder as NormalHolder
                configNormalPost(normalHolder, item)
                fillNormalPostButtonsBlock(normalHolder, item)
            }

            TYPE_SCHEDULED -> {
                val scheludedHolder = viewHolder as ScheludedHolder
                configNormalPost(scheludedHolder, item)
                bindScheludedButtonsBlock(scheludedHolder, item)
            }

            TYPE_DELETED -> bindDeleted(viewHolder as DeletedHolder, item)
        }
    }

    fun setNonPublishedPostActionListener(listener: NonPublishedPostActionListener?) {
        nonPublishedPostActionListener = listener
    }

    private fun bindScheludedButtonsBlock(holder: ScheludedHolder, post: Post) {
        holder.deleteButton.setOnClickListener {
            nonPublishedPostActionListener?.onButtonRemoveClick(post)
        }
    }

    private fun bindDeleted(holder: DeletedHolder, item: Post) {
        holder.bRestore.setOnClickListener { clickListener.onRestoreClick(item) }
    }

    @SuppressLint("SetTextI18n")
    private fun configNormalPost(holder: AbsPostHolder, post: Post) {
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
        attachmentsViewBinder.displayAttachments(
            post.attachments,
            holder.attachmentContainers,
            false,
            null, null
        )
        attachmentsViewBinder.displayCopyHistory(
            post.getCopyHierarchy(),
            holder.attachmentContainers.vgPosts,
            true,
            R.layout.item_copy_history_post
        )
        holder.tvOwnerName.text = post.authorName
        val reduced = AppTextUtils.reduceStringForPost(post.text)
        holder.tvText.text =
            OwnerLinkSpanFactory.withSpans(
                reduced,
                owners = true,
                topics = false,
                listener = mLinkActionAdapter
            )
        holder.tvShowMore.visibility =
            if (post.hasText() && post.text?.length.orZero() > 400) View.VISIBLE else View.GONE
        holder.tvText.visibility = if (post.hasText()) View.VISIBLE else View.GONE
        holder.vTextContainer.visibility = if (post.hasText()) View.VISIBLE else View.GONE
        val ownerAvaUrl = post.authorPhoto
        displayAvatar(holder.ivOwnerAvatar, transformation, ownerAvaUrl, Constants.PICASSO_TAG)
        holder.ivOwnerAvatar.setOnClickListener {
            clickListener.onAvatarClick(
                post.authorId
            )
        }
        holder.ivFriendOnly.visibility = if (post.isFriendsOnly) View.VISIBLE else View.GONE
        val displaySigner = post.signerId > 0 && post.creator != null
        holder.vSignerRoot.visibility = if (displaySigner) View.VISIBLE else View.GONE
        if (displaySigner) {
            holder.tvSignerName.text = post.creator?.fullName
            displayAvatar(
                holder.ivSignerIcon,
                transformation,
                post.creator?.get100photoOrSmaller(),
                Constants.PICASSO_TAG
            )
            holder.vSignerRoot.setOnClickListener {
                clickListener.onAvatarClick(
                    post.signerId
                )
            }
        }
        holder.root.setOnClickListener { clickListener.onPostClick(post) }
        holder.topDivider.visibility = View.GONE
        if (holder.viewCounter != null) {
            holder.viewCounter.visibility = if (post.viewCount > 0) View.VISIBLE else View.GONE
            holder.viewCounter.text = post.viewCount.toString()
        }
    }

    private fun fillNormalPostButtonsBlock(holder: NormalHolder, post: Post) {
        holder.pinRoot.visibility = if (post.isPinned) View.VISIBLE else View.GONE
        val formattedDate = AppTextUtils.getDateFromUnixTime(mContext, post.date)
        var postSubtitle = formattedDate
        post.source.requireNonNull {
            when (it.getData()) {
                VKApiPostSource.Data.PROFILE_ACTIVITY -> postSubtitle =
                    mContext.getString(R.string.updated_status_at, formattedDate)

                VKApiPostSource.Data.PROFILE_PHOTO -> postSubtitle =
                    mContext.getString(R.string.updated_profile_photo_at, formattedDate)
            }
        }
        holder.tvTime.text = postSubtitle
        holder.likeButton.setIcon(if (post.isUserLikes) R.drawable.heart_filled else R.drawable.heart)
        holder.likeButton.isActive = post.isUserLikes
        holder.likeButton.count = post.likesCount
        holder.likeButton.setOnClickListener { clickListener.onLikeClick(post) }
        holder.likeButton.setOnLongClickListener {
            clickListener.onLikeLongClick(post)
            true
        }
        holder.commentsButton.visibility =
            if (post.isCanPostComment || post.commentsCount > 0) View.VISIBLE else View.INVISIBLE
        holder.commentsButton.count = post.commentsCount
        holder.commentsButton.setOnClickListener { clickListener.onCommentsClick(post) }
        holder.shareButton.isActive = post.isUserReposted
        holder.shareButton.count = post.repostCount
        holder.shareButton.setOnClickListener { clickListener.onShareClick(post) }
        holder.shareButton.setOnLongClickListener {
            clickListener.onShareLongClick(post)
            true
        }
    }

    override fun viewHolder(view: View, type: Int): RecyclerView.ViewHolder {
        when (type) {
            TYPE_NORMAL -> return NormalHolder(view)
            TYPE_DELETED -> return DeletedHolder(view)
            TYPE_SCHEDULED -> return ScheludedHolder(view)
        }
        throw IllegalArgumentException()
    }

    override fun layoutId(type: Int): Int {
        when (type) {
            TYPE_DELETED -> return R.layout.item_post_deleted
            TYPE_NORMAL -> return R.layout.item_post_normal
            TYPE_SCHEDULED -> return R.layout.item_post_scheduled
        }
        throw IllegalArgumentException()
    }

    fun setOnHashTagClickListener(onHashTagClickListener: EmojiconTextView.OnHashTagClickListener?) {
        mOnHashTagClickListener = onHashTagClickListener
    }

    override fun getItemType(position: Int): Int {
        val post = getItem(position - headersCount)
        if (post.isDeleted) {
            return TYPE_DELETED
        }
        return if (Utils.intValueIn(
                post.postType,
                VKApiPost.Type.POSTPONE,
                VKApiPost.Type.SUGGEST
            )
        ) TYPE_SCHEDULED else TYPE_NORMAL
    }

    interface NonPublishedPostActionListener {
        fun onButtonRemoveClick(post: Post)
    }

    interface ClickListener {
        fun onAvatarClick(ownerId: Long)
        fun onShareClick(post: Post)
        fun onPostClick(post: Post)
        fun onRestoreClick(post: Post)
        fun onCommentsClick(post: Post)
        fun onLikeLongClick(post: Post)
        fun onShareLongClick(post: Post)
        fun onLikeClick(post: Post)
    }

    private class DeletedHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val bRestore: Button = itemView.findViewById(R.id.item_post_deleted_restore)
    }

    private abstract inner class AbsPostHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val root: View = itemView.findViewById(R.id.card_view)
        val topDivider: View = itemView.findViewById(R.id.top_divider)
        val tvOwnerName: TextView = itemView.findViewById(R.id.item_post_owner_name)
        val ivOwnerAvatar: ImageView = itemView.findViewById(R.id.item_post_avatar)
        val vTextContainer: View = itemView.findViewById(R.id.item_text_container)
        val tvText: EmojiconTextView = itemView.findViewById(R.id.item_post_text)
        val tvShowMore: TextView
        val tvTime: TextView
        val ivFriendOnly: ImageView
        val viewCounter: TextView?
        val vSignerRoot: View
        val ivSignerIcon: ImageView
        val tvSignerName: TextView
        val attachmentContainers: AttachmentsHolder
        val tvCopyright: TextView

        init {
            tvText.setOnHashTagClickListener(mOnHashTagClickListener)
            tvShowMore = itemView.findViewById(R.id.item_post_show_more)
            tvTime = itemView.findViewById(R.id.item_post_time)
            ivFriendOnly = itemView.findViewById(R.id.item_post_friends_only)
            vSignerRoot = itemView.findViewById(R.id.item_post_signer_root)
            ivSignerIcon = itemView.findViewById(R.id.item_post_signer_icon)
            tvSignerName = itemView.findViewById(R.id.item_post_signer_name)
            attachmentContainers = forPost((itemView as ViewGroup))
            viewCounter = itemView.findViewById(R.id.post_views_counter)
            tvCopyright = itemView.findViewById(R.id.item_post_copyright)
        }
    }

    private inner class NormalHolder(view: View) : AbsPostHolder(view) {
        val pinRoot: View = root.findViewById(R.id.item_post_normal_pin)
        val likeButton: CircleCounterButton = root.findViewById(R.id.like_button)
        val shareButton: CircleCounterButton = root.findViewById(R.id.share_button)
        val commentsButton: CircleCounterButton = root.findViewById(R.id.comments_button)

    }

    private inner class ScheludedHolder(view: View) : AbsPostHolder(view) {
        val deleteButton: CircleCounterButton = root.findViewById(R.id.button_delete)
    }

    companion object {
        private const val TYPE_SCHEDULED = 2
        private const val TYPE_DELETED = 1
        private const val TYPE_NORMAL = 0
    }

    init {
        mLinkActionAdapter = object : LinkActionAdapter() {
            override fun onOwnerClick(ownerId: Long) {
                clickListener.onAvatarClick(ownerId)
            }
        }
    }
}