package dev.ragnarok.fenrir.fragment.friends.allfriends

import android.content.Context
import android.graphics.Color
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
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UsersPart
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.UserInfoResolveUtil
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon

class FriendsRecycleAdapter(private var data: List<UsersPart>, private val context: Context) :
    RecyclerView.Adapter<FriendsRecycleAdapter.Holder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var group = false
    private var listener: Listener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_new_user, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val itemInfo = get(position)
        val user = itemInfo.user
        val headerVisible = group && itemInfo.first
        holder.header.visibility = if (headerVisible) View.VISIBLE else View.GONE
        if (headerVisible) {
            holder.headerCount.text = itemInfo.fullSectionCount.toString()
            holder.headerTitle.setText(itemInfo.sectionTitleRes)
        }
        holder.name.text = user.fullName
        holder.name.setTextColor(Utils.getVerifiedColor(context, user.isVerified))
        holder.status.text = UserInfoResolveUtil.getUserActivityLine(context, user, true)
        holder.status.setTextColor(if (user.isOnline) CurrentTheme.getColorPrimary(context) else STATUS_COLOR_OFFLINE)
        holder.online.visibility = if (user.isOnline) View.VISIBLE else View.GONE
        val onlineIcon =
            getOnlineIcon(user.isOnline, user.isOnlineMobile, user.platform, user.onlineApp)
        if (onlineIcon != null) {
            holder.online.setImageResource(onlineIcon)
        }
        displayAvatar(holder.avatar, transformation, user.maxSquareAvatar, Constants.PICASSO_TAG)
        holder.itemView.setOnClickListener {
            listener?.onUserClick(user)
        }
        addSelectionProfileSupport(context, holder.avatarRoot, user)
        holder.ivVerified.visibility = if (user.isVerified) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int {
        var count = 0
        for (pair in data) {
            if (!pair.enable) {
                continue
            }
            count += pair.users.size
        }
        return count
    }

    @Throws(IllegalArgumentException::class)
    private operator fun get(position: Int): ItemInfo {
        var offset = 0
        for (pair in data) {
            if (!pair.enable) {
                continue
            }
            val newOffset = offset + pair.users.size
            if (position < newOffset) {
                val internalPosition = position - offset
                val first = internalPosition == 0
                val displayCount =
                    if (pair.displayCount == null) pair.users.size else pair.displayCount.orZero()
                return ItemInfo(pair.users[internalPosition], first, displayCount, pair.titleResId)
            }
            offset = newOffset
        }
        throw IllegalArgumentException("Invalid adapter position")
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun setGroup(group: Boolean) {
        this.group = group
    }

    fun setData(data: List<UsersPart>, grouping: Boolean) {
        this.data = data
        group = grouping
        notifyDataSetChanged()
    }

    interface Listener {
        fun onUserClick(user: User)
    }

    private class ItemInfo(
        val user: User,
        val first: Boolean,
        val fullSectionCount: Int,
        val sectionTitleRes: Int
    )

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: View = itemView.findViewById(R.id.header)
        val headerTitle: TextView = itemView.findViewById(R.id.title)
        val headerCount: TextView = itemView.findViewById(R.id.count)
        val name: TextView = itemView.findViewById(R.id.item_friend_name)
        val status: TextView = itemView.findViewById(R.id.item_friend_status)
        val avatarRoot: ViewGroup = itemView.findViewById(R.id.item_friend_avatar_container)
        val avatar: ImageView = itemView.findViewById(R.id.item_friend_avatar)
        val online: ImageView = itemView.findViewById(R.id.item_friend_online)
        val ivVerified: ImageView = itemView.findViewById(R.id.item_verified)

        init {
            Utils.setColorFilter(
                online, CurrentTheme.getColorPrimary(
                    context
                )
            )
        }
    }

    companion object {
        private val STATUS_COLOR_OFFLINE = Color.parseColor("#999999")
    }

}