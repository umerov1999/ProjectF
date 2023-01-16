package dev.ragnarok.fenrir.fragment.communitycontrol.communityban

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.link.internal.LinkActionAdapter
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.FormatUtil
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView

class CommunityBannedAdapter(private var data: List<Banned>) :
    RecyclerView.Adapter<CommunityBannedAdapter.Holder>() {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val ownerLinkActionListener: OwnerLinkSpanFactory.ActionListener = LinkActionAdapter()
    private var actionListener: ActionListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_community_ban_info, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val context = holder.itemView.context
        val banned = data[position]
        val bannedOwner = banned.banned
        val admin = banned.admin
        val info = banned.info
        holder.name.text = bannedOwner.fullName
        displayAvatar(
            holder.avatar,
            transformation,
            bannedOwner.maxSquareAvatar,
            Constants.PICASSO_TAG
        )
        var onlineViewRes: Int? = null
        if (bannedOwner is User) {
            onlineViewRes =
                getOnlineIcon(
                    bannedOwner.isOnline,
                    bannedOwner.isOnlineMobile,
                    bannedOwner.platform,
                    bannedOwner.onlineApp
                )
        }
        if (onlineViewRes != null) {
            holder.onlineView.setIcon(onlineViewRes)
            holder.onlineView.visibility = View.VISIBLE
        } else {
            holder.onlineView.visibility = View.GONE
        }
        val comment = info.comment
        if (comment.nonNullNoEmpty()) {
            holder.comment.visibility = View.VISIBLE
            val commentText = context.getString(R.string.ban_comment_text, comment)
            holder.comment.text = commentText
        } else {
            holder.comment.visibility = View.GONE
        }
        val spannable = FormatUtil.formatCommunityBanInfo(
            context, admin.getOwnerObjectId(),
            admin.fullName, info.endDate, ownerLinkActionListener
        )
        holder.dateAndAdminInfo.movementMethod = LinkMovementMethod.getInstance()
        holder.dateAndAdminInfo.setText(spannable, TextView.BufferType.SPANNABLE)
        holder.itemView.setOnClickListener {
            actionListener?.onBannedClick(banned)
        }
        holder.itemView.setOnLongClickListener {
            actionListener?.onBannedLongClick(banned)
            true
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Banned>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    interface ActionListener {
        fun onBannedClick(banned: Banned)
        fun onBannedLongClick(banned: Banned)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        val onlineView: OnlineView = itemView.findViewById(R.id.online)
        val name: TextView = itemView.findViewById(R.id.name)
        val dateAndAdminInfo: TextView = itemView.findViewById(R.id.date_and_admin_info)
        val comment: TextView = itemView.findViewById(R.id.comment_text)
    }
}