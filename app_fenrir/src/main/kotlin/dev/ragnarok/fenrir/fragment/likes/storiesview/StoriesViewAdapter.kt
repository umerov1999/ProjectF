package dev.ragnarok.fenrir.fragment.likes.storiesview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SelectionUtils.addSelectionProfileSupport
import dev.ragnarok.fenrir.fragment.communities.CommunitiesAdapter.Companion.getCommunityType
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.toColor
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.UserInfoResolveUtil
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.view.OnlineView

class StoriesViewAdapter(
    private val mContext: Context,
    private var mData: List<Pair<Owner, Boolean>>
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val transformationWithStory: Transformation =
        CurrentTheme.createTransformationStrokeForAvatar()
    private var mClickListener: ClickListener? = null
    private var longClickListener: LongClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            TYPE_USER -> return PeopleHolder(
                LayoutInflater.from(mContext).inflate(R.layout.item_people_with_like, parent, false)
            )

            TYPE_COMMUNITY -> return CommunityHolder(
                LayoutInflater.from(mContext).inflate(R.layout.item_group_with_like, parent, false)
            )
        }
        throw RuntimeException("StoriesViewAdapter.onCreateViewHolder")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_USER -> bindUserHolder(
                holder as PeopleHolder,
                mData[position].first as User,
                mData[position].second
            )

            TYPE_COMMUNITY -> bindCommunityHolder(
                holder as CommunityHolder,
                mData[position].first as Community,
                mData[position].second
            )
        }
    }

    private fun bindCommunityHolder(holder: CommunityHolder, community: Community, liked: Boolean) {
        holder.tvName.text = community.fullName
        holder.tvName.setTextColor(Utils.getVerifiedColor(mContext, community.isVerified))
        holder.ivVerified.visibility = if (community.isVerified) View.VISIBLE else View.GONE
        holder.liked.visibility = if (liked) View.VISIBLE else View.GONE
        holder.tvType.text = getCommunityType(mContext, community)
        holder.tvMembersCount.text = mContext.getString(
            R.string.members_count,
            if (community.membersCount > 0) AppTextUtils.getCounterWithK(community.membersCount) else "-"
        )
        with()
            .load(community.maxSquareAvatar)
            .tag(Constants.PICASSO_TAG)
            .transform(if (community.hasUnseenStories) transformationWithStory else transformation)
            .into(holder.ivAvatar)
        holder.itemView.setOnClickListener {
            mClickListener?.onOwnerClick(community)
        }
        holder.itemView.setOnLongClickListener {
            longClickListener?.onOwnerLongClick(
                community
            ) == true
        }
    }

    private fun bindUserHolder(holder: PeopleHolder, user: User, liked: Boolean) {
        holder.name.text = user.fullName
        holder.name.setTextColor(Utils.getVerifiedColor(mContext, user.isVerified))
        holder.ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE
        holder.blacklisted.visibility = if (user.blacklisted) View.VISIBLE else View.GONE
        holder.subtitle.text = UserInfoResolveUtil.getUserActivityLine(mContext, user, true)
        holder.subtitle.setTextColor(if (user.isOnline) CurrentTheme.getColorPrimary(mContext) else STATUS_COLOR_OFFLINE)
        holder.liked.visibility = if (liked) View.VISIBLE else View.GONE
        holder.online.visibility = if (user.isOnline) View.VISIBLE else View.GONE
        val onlineIcon =
            ViewUtils.getOnlineIcon(
                user.isOnline,
                user.isOnlineMobile,
                user.platform,
                user.onlineApp
            )
        holder.online.setIcon(onlineIcon ?: 0)
        val avaUrl = user.maxSquareAvatar
        displayAvatar(
            holder.avatar,
            if (user.hasUnseenStories) transformationWithStory else transformation,
            avaUrl,
            Constants.PICASSO_TAG,
            monochrome = user.blacklisted
        )
        holder.itemView.setOnClickListener {
            mClickListener?.onOwnerClick(user)
        }
        holder.itemView.setOnLongClickListener {
            longClickListener?.onOwnerLongClick(
                user
            ) == true
        }
        addSelectionProfileSupport(mContext, holder.avatarRoot, user)
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setItems(data: List<Pair<Owner, Boolean>>) {
        mData = data
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (mData[position].first is User) TYPE_USER else TYPE_COMMUNITY
    }

    fun setClickListener(clickListener: ClickListener?) {
        mClickListener = clickListener
    }

    interface ClickListener {
        fun onOwnerClick(owner: Owner)
    }

    interface LongClickListener {
        fun onOwnerLongClick(owner: Owner): Boolean
    }

    private class CommunityHolder(root: View) : RecyclerView.ViewHolder(root) {
        val tvName: TextView = root.findViewById(R.id.item_group_name)
        val tvType: TextView = root.findViewById(R.id.item_group_type)
        val tvMembersCount: TextView = root.findViewById(R.id.item_group_members)
        val ivAvatar: ImageView = root.findViewById(R.id.item_group_avatar)
        val ivVerified: ImageView = itemView.findViewById(R.id.item_verified)
        val liked: ImageView = itemView.findViewById(R.id.item_group_liked)
    }

    private class PeopleHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.item_people_name)
        val subtitle: TextView = itemView.findViewById(R.id.item_people_subtitle)
        val avatar: ImageView = itemView.findViewById(R.id.item_people_avatar)
        val liked: ImageView = itemView.findViewById(R.id.item_people_liked)
        val ivVerified: ImageView = itemView.findViewById(R.id.item_verified)
        val avatarRoot: ViewGroup = itemView.findViewById(R.id.avatar_root)
        val blacklisted: ImageView = itemView.findViewById(R.id.item_blacklisted)
        val online: OnlineView = itemView.findViewById(R.id.item_people_online)
    }

    companion object {
        private val STATUS_COLOR_OFFLINE = "#999999".toColor()
        private const val TYPE_USER = 0
        private const val TYPE_COMMUNITY = 1
    }

}
