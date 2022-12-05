package dev.ragnarok.fenrir.fragment.communitycontrol.communitymanagers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.ifNonNullNoEmpty
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar
import dev.ragnarok.fenrir.util.ViewUtils.getOnlineIcon
import dev.ragnarok.fenrir.view.OnlineView

class CommunityManagersAdapter(private var users: List<Manager>) :
    RecyclerView.Adapter<CommunityManagersAdapter.Holder>() {
    companion object {
        private val roleTextResources: MutableMap<String, Int> = HashMap(4)

        init {
            roleTextResources["moderator"] = R.string.role_moderator
            roleTextResources["editor"] = R.string.role_editor
            roleTextResources["administrator"] =
                R.string.role_administrator
            roleTextResources["creator"] = R.string.role_creator
        }
    }

    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var actionListener: ActionListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_community_manager, parent, false)
        )
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val manager = users[position]
        val user = manager.user
        holder.name.text = user?.fullName
        displayAvatar(holder.avatar, transformation, user?.maxSquareAvatar, Constants.PICASSO_TAG)
        val onlineRes =
            getOnlineIcon(
                user?.isOnline == true,
                user?.isOnlineMobile == true,
                user?.platform.orZero(),
                user?.onlineApp.orZero()
            )
        if (onlineRes != null) {
            holder.onlineView.setIcon(onlineRes)
            holder.onlineView.visibility = View.VISIBLE
        } else {
            holder.onlineView.visibility = View.GONE
        }
        roleTextResources[manager.role]?.let {
            holder.role.setText(it)
        } ?: run {
            manager.contactInfo?.getDescription().ifNonNullNoEmpty({
                holder.role.text = it
            }, {
                holder.role.setText(R.string.role_unknown)
            })
        }
        holder.itemView.setOnClickListener {
            actionListener?.onManagerClick(manager)
        }
        holder.itemView.setOnLongClickListener {
            if ("creator".equals(manager.role, ignoreCase = true)) {
                return@setOnLongClickListener false
            }
            actionListener?.onManagerLongClick(manager)
            true
        }
    }

    override fun getItemCount(): Int {
        return users.size
    }

    fun setData(data: List<Manager>) {
        users = data
        notifyDataSetChanged()
    }

    interface ActionListener {
        fun onManagerClick(manager: Manager)
        fun onManagerLongClick(manager: Manager)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        val onlineView: OnlineView = itemView.findViewById(R.id.online)
        val name: TextView = itemView.findViewById(R.id.name)
        val role: TextView = itemView.findViewById(R.id.role)
    }
}