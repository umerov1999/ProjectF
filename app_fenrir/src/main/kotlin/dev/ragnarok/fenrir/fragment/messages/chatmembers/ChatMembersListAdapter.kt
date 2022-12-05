package dev.ragnarok.fenrir.fragment.messages.chatmembers

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.AppChatUser
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UserPlatform
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView

class ChatMembersListAdapter(context: Context, private var data: List<AppChatUser>) :
    RecyclerView.Adapter<ChatMembersListAdapter.ViewHolder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val paddingForFirstLast: Int = if (Utils.is600dp(context)) Utils.dpToPx(16f, context)
        .toInt() else 0
    private var actionListener: ActionListener? = null
    private var recyclerView: RecyclerView? = null
    private var isOwner = false
    fun setIsOwner(isOwner: Boolean) {
        this.isOwner = isOwner
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.item_chat_user_list, viewGroup, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val item = data[position]
        val user = item.getMember()
        var online = false
        var onlineMobile = false
        @UserPlatform var platform = UserPlatform.UNKNOWN
        var app = 0
        if (user is User) {
            online = user.isOnline
            onlineMobile = user.isOnlineMobile
            platform = user.platform
            app = user.onlineApp
        }
        val iconRes = getOnlineIcon(online, onlineMobile, platform, app)
        holder.vOnline.setIcon(iconRes ?: 0)
        holder.vOnline.visibility = if (online) View.VISIBLE else View.GONE
        val userAvatarUrl = user?.maxSquareAvatar
        if (userAvatarUrl.isNullOrEmpty()) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .transform(transformation)
                .into(holder.ivAvatar)
        } else {
            with()
                .load(userAvatarUrl)
                .transform(transformation)
                .into(holder.ivAvatar)
        }
        holder.tvName.text = user?.fullName
        if (item.isOwner()) {
            holder.tvInvitedBy.visibility = View.GONE
            holder.tvAdmin.visibility = View.VISIBLE
            holder.tvAdmin.setText(R.string.creator_of_conversation)
            holder.tvInvitedDate.visibility = View.GONE
        } else if (item.isAdmin()) {
            holder.tvAdmin.visibility = View.VISIBLE
            holder.tvAdmin.setText(R.string.role_administrator)
            holder.tvInvitedBy.visibility = View.VISIBLE
            holder.tvInvitedBy.text =
                context.getString(R.string.invited_by, item.getInviter()?.fullName)
            if (item.getJoin_date() > 0) {
                holder.tvInvitedDate.text =
                    AppTextUtils.getDateFromUnixTime(context, item.getJoin_date())
            } else {
                holder.tvInvitedDate.visibility = View.GONE
            }
        } else {
            holder.tvInvitedBy.visibility = View.VISIBLE
            holder.tvInvitedBy.text =
                context.getString(R.string.invited_by, item.getInviter()?.fullName)
            holder.tvAdmin.visibility = View.GONE
            if (item.getJoin_date() > 0) {
                holder.tvInvitedDate.text =
                    AppTextUtils.getDateFromUnixTime(context, item.getJoin_date())
            } else {
                holder.tvInvitedDate.visibility = View.GONE
            }
        }
        user?.domain.ifNonNullNoEmpty({
            holder.tvDomain.text = "@$it"
        }, {
            holder.tvDomain.text = "@id" + user?.ownerId.orZero()
        })
        holder.itemView.setOnClickListener {
            actionListener?.onUserClick(item)
        }
        holder.vRemove.visibility = if (item.isCanRemove()) View.VISIBLE else View.GONE
        holder.vRemove.setOnClickListener {
            actionListener?.onRemoveClick(item)
        }
        val view = holder.itemView
        view.setPadding(
            view.paddingLeft,
            if (position == 0) paddingForFirstLast else 0,
            view.paddingRight,
            if (position == itemCount - 1) paddingForFirstLast else 0
        )
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<AppChatUser>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    interface ActionListener {
        fun onRemoveClick(user: AppChatUser)
        fun onUserClick(user: AppChatUser)
        fun onAdminToggleClick(isAdmin: Boolean, ownerId: Int)
    }

    inner class ViewHolder internal constructor(root: View) : RecyclerView.ViewHolder(root),
        OnCreateContextMenuListener {
        val vOnline: OnlineView
        val ivAvatar: ImageView
        val tvName: TextView
        val tvDomain: TextView
        val tvInvitedBy: TextView
        val tvInvitedDate: TextView
        val tvAdmin: TextView
        val vRemove: View
        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
            val position = recyclerView?.getChildAdapterPosition(v) ?: 0
            val item = data[position]
            if (isOwner && !item.isOwner()) {
                menu.add(
                    0,
                    v.id,
                    0,
                    if (item.isAdmin()) R.string.disrate else R.string.assign_administrator
                ).setOnMenuItemClickListener {
                    actionListener?.onAdminToggleClick(!item.isAdmin(), item.getObjectId())
                    true
                }
            }
        }

        init {
            itemView.setOnCreateContextMenuListener(this)
            vOnline = root.findViewById(R.id.item_user_online)
            ivAvatar = root.findViewById(R.id.item_user_avatar)
            tvName = root.findViewById(R.id.item_user_name)
            tvInvitedBy = root.findViewById(R.id.item_user_invited_by)
            vRemove = root.findViewById(R.id.item_user_remove)
            tvDomain = root.findViewById(R.id.item_user_domain)
            tvInvitedDate = root.findViewById(R.id.item_user_invited_time)
            tvAdmin = root.findViewById(R.id.item_user_admin)
        }
    }

}