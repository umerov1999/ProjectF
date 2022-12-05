package dev.ragnarok.fenrir.fragment.groupchats

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.GroupChats
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import java.util.*

class GroupChatsAdapter(
    context: Context,
    chats: MutableList<GroupChats>,
    private val mActionListener: ActionListener
) : RecyclerBindableAdapter<GroupChats, GroupChatsAdapter.ViewHolder>(chats) {
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private var firstLastPadding = 0
    override fun onBindItemViewHolder(viewHolder: ViewHolder, position: Int, type: Int) {
        val item = getItem(position - headersCount)
        val context = viewHolder.itemView.context
        viewHolder.title.text = item.getTitle()
        viewHolder.subtitle.text = context.getString(
            R.string.group_chats_counter,
            AppTextUtils.getDateFromUnixTime(item.getLastUpdateTime()), item.getMembers_count()
        )
        viewHolder.itemView.setOnLongClickListener {
            val popup = PopupMenu(context, viewHolder.itemView)
            popup.inflate(R.menu.topics_item_menu)
            popup.setOnMenuItemClickListener { item1: MenuItem ->
                if (item1.itemId == R.id.copy_url) {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip =
                        ClipData.newPlainText(
                            context.getString(R.string.link),
                            item.getInvite_link()
                        )
                    clipboard?.setPrimaryClip(clip)
                    createCustomToast(context).showToast(R.string.copied)
                    return@setOnMenuItemClickListener true
                }
                false
            }
            popup.show()
            false
        }
        val avaUrl = item.getPhoto()
        if (avaUrl.isNullOrEmpty()) {
            with()
                .load(R.drawable.ic_avatar_unknown)
                .transform(transformation)
                .into(viewHolder.ava)
        } else {
            with()
                .load(avaUrl)
                .transform(transformation)
                .into(viewHolder.ava)
        }
        viewHolder.itemView.setPadding(
            viewHolder.itemView.paddingLeft,
            if (position == 0) firstLastPadding else 0,
            viewHolder.itemView.paddingRight,
            if (position == itemCount - 1) firstLastPadding else 0
        )
        viewHolder.itemView.setOnClickListener {
            mActionListener.onGroupChatsClick(
                item
            )
        }
    }

    override fun viewHolder(view: View, type: Int): ViewHolder {
        return ViewHolder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_group_chat
    }

    interface ActionListener : EventListener {
        fun onGroupChatsClick(chat: GroupChats)
    }

    class ViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val title: TextView = root.findViewById(R.id.item_group_chat_title)
        val subtitle: TextView = root.findViewById(R.id.item_group_chat_subtitle)
        val ava: ImageView = root.findViewById(R.id.item_group_chat_avatar)
    }

    init {
        if (Utils.is600dp(context)) {
            firstLastPadding = Utils.dpToPx(16f, context).toInt()
        }
    }
}