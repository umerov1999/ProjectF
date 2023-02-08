package dev.ragnarok.fenrir.fragment.feed

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
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder
import dev.ragnarok.fenrir.fragment.base.AttachmentsHolder.Companion.forPost
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder
import dev.ragnarok.fenrir.fragment.base.AttachmentsViewBinder.OnAttachmentsActionCallback
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.fragment.base.holder.IdentificableHolder
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.News
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.setCountText
import dev.ragnarok.fenrir.view.CircleCounterButton

class FeedAdapter(
    private val context: Context,
    data: MutableList<News>,
    attachmentsActionCallback: OnAttachmentsActionCallback
) : RecyclerBindableAdapter<News, FeedAdapter.PostHolder>(data) {
    private val attachmentsViewBinder: AttachmentsViewBinder =
        AttachmentsViewBinder(context, attachmentsActionCallback)
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var clickListener: ClickListener? = null
    private var nextHolderId = 0
    private var recyclerView: RecyclerView? = null

    @SuppressLint("SetTextI18n")
    override fun onBindItemViewHolder(viewHolder: PostHolder, position: Int, type: Int) {
        val item = getItem(position)
        viewHolder.tvDonut.visibility = if (item.isDonut) View.VISIBLE else View.GONE
        attachmentsViewBinder.displayAttachments(
            item.attachments,
            viewHolder.attachmentsHolder,
            false,
            null, null
        )
        attachmentsViewBinder.displayCopyHistory(
            item.getCopyHistory(),
            viewHolder.attachmentsHolder.vgPosts,
            true,
            R.layout.item_copy_history_post
        )
        attachmentsViewBinder.displayFriendsPost(
            item.friends,
            viewHolder.attachmentsHolder.vgFriends,
            R.layout.item_friend_link
        )
        viewHolder.tvOwnerName.text = item.ownerName
        val result = AppTextUtils.reduceStringForPost(item.text)
        viewHolder.tvText.text =
            OwnerLinkSpanFactory.withSpans(
                result,
                owners = true,
                topics = false,
                listener = object : LinkActionAdapter() {
                    override fun onOwnerClick(ownerId: Long) {
                        clickListener?.onAvatarClick(ownerId)
                    }
                })
        var force = false
        if (item.text.isNullOrEmpty()) {
            when (item.type) {
                "photo" -> {
                    force = true
                    viewHolder.tvText.setText(R.string.public_photo)
                }

                "wall_photo" -> {
                    force = true
                    viewHolder.tvText.setText(R.string.public_photo_wall)
                }

                "photo_tag" -> {
                    force = true
                    viewHolder.tvText.setText(R.string.public_photo_tag)
                }

                "friend" -> {
                    force = true
                    viewHolder.tvText.setText(R.string.public_friends)
                }

                "audio" -> {
                    force = true
                    viewHolder.tvText.setText(R.string.public_audio)
                }

                "video" -> {
                    force = true
                    viewHolder.tvText.setText(R.string.public_video)
                }
            }
        }
        viewHolder.bottomActionsContainer.visibility =
            if (item.type == "post") View.VISIBLE else View.GONE
        viewHolder.tvShowMore.visibility =
            if (Utils.safeLenghtOf(item.text) > 400) View.VISIBLE else View.GONE

        /*
        if (item.getSource() != null){
            switch (item.getSource().data){
                case PROFILE_ACTIVITY:
                    postSubtitle = context.getString(R.string.updated_status_at) + SPACE + AppTextUtils.getDateFromUnixTime(context, item.getDate());
                    break;
                case PROFILE_PHOTO:
                    postSubtitle = context.getString(R.string.updated_profile_photo_at) + SPACE + AppTextUtils.getDateFromUnixTime(context, item.getDate());
                    break;
            }
        }
        */
        val postTime = AppTextUtils.getDateFromUnixTime(context, item.date)
        viewHolder.tvTime.text = postTime
        viewHolder.tvText.visibility =
            if (item.text.isNullOrEmpty() && !force) View.GONE else View.VISIBLE
        val ownerAvaUrl = item.ownerMaxSquareAvatar
        displayAvatar(viewHolder.ivOwnerAvatar, transformation, ownerAvaUrl, Constants.PICASSO_TAG)
        viewHolder.ivOwnerAvatar.setOnClickListener {
            clickListener?.onAvatarClick(item.sourceId)
        }
        fillCounters(viewHolder, item)
        viewHolder.cardView.setOnClickListener {
            clickListener?.onPostClick(item)
        }
        viewHolder.viewsCounter.visibility = if (item.viewCount > 0) View.VISIBLE else View.GONE
        //holder.viewsCounter.setText(String.valueOf(item.getViewCount()));
        setCountText(viewHolder.viewsCounter, item.viewCount, false)

        item.copyright?.let { vit ->
            viewHolder.tvCopyright.visibility = View.VISIBLE
            viewHolder.tvCopyright.text = "©" + vit.name
            viewHolder.tvCopyright.setOnClickListener {
                LinkHelper.openUrl(
                    viewHolder.tvCopyright.context as Activity,
                    Settings.get().accounts().current,
                    vit.link,
                    false
                )
            }
        } ?: run { viewHolder.tvCopyright.visibility = View.GONE }
    }

    override fun viewHolder(view: View, type: Int): PostHolder {
        return PostHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_feed
    }

    internal fun generateHolderId(): Int {
        nextHolderId++
        return nextHolderId
    }

    private fun fillCounters(holder: PostHolder, news: News) {
        val targetLikeRes = if (news.isUserLike) R.drawable.heart_filled else R.drawable.heart
        holder.likeButton.setIcon(targetLikeRes)
        holder.likeButton.isActive = news.isUserLike
        holder.likeButton.count = news.likeCount
        holder.likeButton.setOnClickListener {
            clickListener?.onLikeClick(news, !news.isUserLike)
        }
        holder.likeButton.setOnLongClickListener {
            clickListener?.onLikeLongClick(
                news
            ) == true
        }
        holder.commentsButton.visibility =
            if (news.isCommentCanPost || news.commentCount > 0) View.VISIBLE else View.INVISIBLE
        holder.commentsButton.count = news.commentCount
        holder.commentsButton.setOnClickListener {
            clickListener?.onCommentButtonClick(news)
        }
        holder.shareButton.isActive = news.isUserReposted
        holder.shareButton.count = news.repostsCount
        holder.shareButton.setOnClickListener {
            clickListener?.onRepostClick(news)
        }
        holder.shareButton.setOnLongClickListener {
            clickListener?.onShareLongClick(
                news
            ) == true
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onAvatarClick(ownerId: Long)
        fun onRepostClick(news: News)
        fun onPostClick(news: News)
        fun onBanClick(news: News)
        fun onIgnoreClick(news: News)
        fun onFaveClick(news: News)
        fun onCommentButtonClick(news: News)
        fun onLikeClick(news: News, add: Boolean)
        fun onLikeLongClick(news: News): Boolean
        fun onShareLongClick(news: News): Boolean
    }

    inner class PostHolder(root: View) : RecyclerView.ViewHolder(root), IdentificableHolder,
        OnCreateContextMenuListener {
        val tvOwnerName: TextView
        val ivOwnerAvatar: ImageView
        val tvText: TextView
        val tvShowMore: TextView
        val tvTime: TextView
        val bottomActionsContainer: Group
        val likeButton: CircleCounterButton
        val shareButton: CircleCounterButton
        val commentsButton: CircleCounterButton
        val attachmentsHolder: AttachmentsHolder
        val viewsCounter: TextView
        val cardView: View
        val tvCopyright: TextView
        val tvDonut: TextView
        override val holderId: Int
            get() = cardView.tag as Int

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = getItemRawPosition(recyclerView?.getChildAdapterPosition(v) ?: 0)
            val feed = getItems()[position]
            if ("post" == feed.type) {
                menu.add(0, v.id, 0, R.string.add_to_bookmarks)
                    .setOnMenuItemClickListener {
                        clickListener?.onFaveClick(feed)
                        true
                    }
            }
            menu.add(0, v.id, 0, R.string.ban_author)
                .setOnMenuItemClickListener {
                    clickListener?.onBanClick(feed)
                    true
                }
            menu.add(0, v.id, 0, R.string.not_interested)
                .setOnMenuItemClickListener {
                    clickListener?.onIgnoreClick(feed)
                    true
                }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
            cardView = root.findViewById(R.id.card_view)
            cardView.tag = generateHolderId()
            ivOwnerAvatar = root.findViewById(R.id.item_post_avatar)
            tvOwnerName = root.findViewById(R.id.item_post_owner_name)
            tvText = root.findViewById(R.id.item_post_text)
            tvShowMore = root.findViewById(R.id.item_post_show_more)
            tvTime = root.findViewById(R.id.item_post_time)
            bottomActionsContainer = root.findViewById(R.id.buttons_bar)
            likeButton = root.findViewById(R.id.like_button)
            commentsButton = root.findViewById(R.id.comments_button)
            shareButton = root.findViewById(R.id.share_button)
            attachmentsHolder = forPost((root as ViewGroup))
            viewsCounter = itemView.findViewById(R.id.post_views_counter)
            tvCopyright = itemView.findViewById(R.id.item_post_copyright)
            tvDonut = itemView.findViewById(R.id.item_need_donate)
        }
    }

}