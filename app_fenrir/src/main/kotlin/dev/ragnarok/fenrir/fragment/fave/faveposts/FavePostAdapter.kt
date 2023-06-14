package dev.ragnarok.fenrir.fragment.fave.faveposts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiPostSource
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
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
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.CircleCounterButton
import dev.ragnarok.fenrir.view.emoji.EmojiconTextView

class FavePostAdapter(
    private val mContext: Context,
    items: MutableList<Post>,
    attachmentsActionCallback: OnAttachmentsActionCallback,
    adapterListener: ClickListener
) : RecyclerBindableAdapter<Post, RecyclerView.ViewHolder>(items) {
    private val attachmentsViewBinder: AttachmentsViewBinder =
        AttachmentsViewBinder(mContext, attachmentsActionCallback)
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val clickListener: ClickListener?
    private val mLinkActionAdapter: LinkActionAdapter
    private var recyclerView: RecyclerView? = null
    private var mOnHashTagClickListener: EmojiconTextView.OnHashTagClickListener? = null
    override fun onBindItemViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int,
        type: Int
    ) {
        val item = getItem(position)
        if (type == TYPE_NORMAL) {
            val normalHolder = viewHolder as NormalHolder
            configNormalPost(normalHolder, item)
            fillNormalPostButtonsBlock(normalHolder, item)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun configNormalPost(holder: AbsPostHolder, post: Post) {
        attachmentsViewBinder.displayAttachments(
            post.attachments,
            holder.attachmentContainers,
            false,
            null,
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
            clickListener?.onAvatarClick(
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
                clickListener?.onAvatarClick(
                    post.signerId
                )
            }
        }
        holder.root.setOnClickListener { clickListener?.onPostClick(post) }
        holder.topDivider.visibility = View.GONE
        if (holder.viewCounter != null) {
            holder.viewCounter.visibility = if (post.viewCount > 0) View.VISIBLE else View.GONE
            holder.viewCounter.text = post.viewCount.toString()
        }
        holder.tvDonut.visibility = if (post.isDonut) View.VISIBLE else View.GONE
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
        holder.likeButton.setOnClickListener { clickListener?.onLikeClick(post) }
        holder.likeButton.setOnLongClickListener {
            clickListener?.onLikeLongClick(post)
            true
        }
        holder.commentsButton.visibility =
            if (post.isCanPostComment || post.commentsCount > 0) View.VISIBLE else View.INVISIBLE
        holder.commentsButton.count = post.commentsCount
        holder.commentsButton.setOnClickListener {
            clickListener?.onCommentsClick(
                post
            )
        }
        holder.shareButton.isActive = post.isUserReposted
        holder.shareButton.count = post.repostCount
        holder.shareButton.setOnClickListener { clickListener?.onShareClick(post) }
        holder.shareButton.setOnLongClickListener {
            clickListener?.onShareLongClick(post)
            true
        }
    }

    override fun viewHolder(view: View, type: Int): RecyclerView.ViewHolder {
        if (type == TYPE_NORMAL) {
            return NormalHolder(view)
        }
        throw IllegalArgumentException()
    }

    override fun layoutId(type: Int): Int {
        if (type == TYPE_NORMAL) {
            return R.layout.item_post_normal
        }
        throw IllegalArgumentException()
    }

    fun setOnHashTagClickListener(onHashTagClickListener: EmojiconTextView.OnHashTagClickListener?) {
        mOnHashTagClickListener = onHashTagClickListener
    }

    override fun getItemType(position: Int): Int {
        return TYPE_NORMAL
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    interface ClickListener {
        fun onAvatarClick(ownerId: Long)
        fun onShareClick(post: Post)
        fun onPostClick(post: Post)
        fun onCommentsClick(post: Post)
        fun onLikeLongClick(post: Post)
        fun onShareLongClick(post: Post)
        fun onLikeClick(post: Post)
        fun onDelete(index: Int, post: Post)
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
        val tvCopyright: TextView
        val tvTime: TextView
        val ivFriendOnly: ImageView
        val viewCounter: TextView?
        val vSignerRoot: View
        val ivSignerIcon: ImageView
        val tvSignerName: TextView
        val attachmentContainers: AttachmentsHolder
        val tvDonut: TextView

        init {
            tvCopyright = itemView.findViewById(R.id.item_post_copyright)
            tvText.setOnHashTagClickListener(mOnHashTagClickListener)
            tvShowMore = itemView.findViewById(R.id.item_post_show_more)
            tvTime = itemView.findViewById(R.id.item_post_time)
            ivFriendOnly = itemView.findViewById(R.id.item_post_friends_only)
            vSignerRoot = itemView.findViewById(R.id.item_post_signer_root)
            ivSignerIcon = itemView.findViewById(R.id.item_post_signer_icon)
            tvSignerName = itemView.findViewById(R.id.item_post_signer_name)
            attachmentContainers = AttachmentsHolder.forPost(itemView as ViewGroup)
            viewCounter = itemView.findViewById(R.id.post_views_counter)
            tvDonut = itemView.findViewById(R.id.item_need_donate)
        }
    }

    private inner class NormalHolder(view: View) : AbsPostHolder(view),
        OnCreateContextMenuListener {
        val pinRoot: View
        val likeButton: CircleCounterButton
        val shareButton: CircleCounterButton
        val commentsButton: CircleCounterButton
        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val post = getItems()[position]
            menu.setHeaderTitle(post.authorName)
            menu.add(0, v.id, 0, R.string.delete).setOnMenuItemClickListener {
                clickListener?.onDelete(position, post)
                true
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
            pinRoot = root.findViewById(R.id.item_post_normal_pin)
            likeButton = root.findViewById(R.id.like_button)
            commentsButton = root.findViewById(R.id.comments_button)
            shareButton = root.findViewById(R.id.share_button)
        }
    }

    companion object {
        private const val TYPE_NORMAL = 0
    }

    init {
        clickListener = adapterListener
        mLinkActionAdapter = object : LinkActionAdapter() {
            override fun onOwnerClick(ownerId: Long) {
                clickListener.onAvatarClick(ownerId)
            }
        }
    }
}