package dev.ragnarok.fenrir.view.navigation

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.squareup.picasso3.Transformation
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.model.drawer.AbsMenuItem
import dev.ragnarok.fenrir.model.drawer.IconMenuItem
import dev.ragnarok.fenrir.model.drawer.NoIconMenuItem
import dev.ragnarok.fenrir.model.drawer.RecentChat
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.Utils

class MenuListAdapter(
    context: Context,
    pageItems: MutableList<AbsMenuItem>,
    private val actionListener: ActionListener,
    private val paging: Boolean
) : RecyclerBindableAdapter<AbsMenuItem, RecyclerView.ViewHolder>(pageItems) {
    private val colorPrimary: Int = CurrentTheme.getColorPrimary(context)
    private val colorSurface: Int = CurrentTheme.getColorSurface(context)
    private val colorOnPrimary: Int = CurrentTheme.getColorOnPrimary(context)
    private val colorOnSurface: Int = CurrentTheme.getColorOnSurface(context)
    private val dp: Int = Utils.dpToPx(1f, context).toInt()
    private val transformation: Transformation = CurrentTheme.createTransformationForAvatar()
    private val noStroke: Boolean = Settings.get().other().is_side_no_stroke()
    override fun onBindItemViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int,
        type: Int
    ) {
        val item = getItem(position)
        viewHolder.itemView.isSelected = item.isSelected
        when (type) {
            AbsMenuItem.TYPE_ICON -> if (paging || !noStroke) {
                bindIconHolder(viewHolder as NormalHolder, item as IconMenuItem)
            } else {
                bindIconNoStrokeHolder(viewHolder as NormalNoStrokeHolder, item as IconMenuItem)
            }
            AbsMenuItem.TYPE_RECENT_CHAT -> bindRecentChat(
                viewHolder as RecentChatHolder,
                item as RecentChat
            )
            AbsMenuItem.TYPE_WITHOUT_ICON -> bindWithoutIcon(
                viewHolder as NoIconHolder,
                item as NoIconMenuItem
            )
        }
    }

    private fun bindWithoutIcon(holder: NoIconHolder, item: NoIconMenuItem) {
        holder.txTitle.setText(item.title)
        holder.txTitle.setTextColor(if (item.isSelected) colorOnPrimary else colorOnSurface)
        holder.contentRoot.setOnClickListener { actionListener.onDrawerItemClick(item) }
        holder.contentRoot.setOnLongClickListener {
            actionListener.onDrawerItemLongClick(item)
            true
        }
    }

    private fun bindIconNoStrokeHolder(holder: NormalNoStrokeHolder, item: IconMenuItem) {
        holder.txtTitle.setText(item.title)
        holder.txtTitle.setTextColor(if (item.isSelected) colorOnPrimary else colorOnSurface)
        holder.tvCount.visibility = if (item.count > 0) View.VISIBLE else View.GONE
        holder.tvCount.text = item.count.toString()
        holder.tvCount.setTextColor(if (item.isSelected) colorOnPrimary else colorPrimary)
        holder.imgIcon.setImageResource(item.icon)
        holder.imgIcon.setColorFilter(if (item.isSelected) colorOnPrimary else colorOnSurface)
        holder.contentRoot.setBackgroundColor(if (item.isSelected) colorPrimary else colorSurface)
        holder.contentRoot.setOnClickListener { actionListener.onDrawerItemClick(item) }
        holder.contentRoot.setOnLongClickListener {
            actionListener.onDrawerItemLongClick(item)
            true
        }
    }

    private fun bindIconHolder(holder: NormalHolder, item: IconMenuItem) {
        holder.txtTitle.setText(item.title)
        holder.txtTitle.setTextColor(if (item.isSelected) colorOnPrimary else colorOnSurface)
        holder.tvCount.visibility = if (item.count > 0) View.VISIBLE else View.GONE
        holder.tvCount.text = item.count.toString()
        holder.tvCount.setTextColor(if (item.isSelected) colorOnPrimary else colorPrimary)
        holder.imgIcon.setImageResource(item.icon)
        holder.imgIcon.setColorFilter(if (item.isSelected) colorOnPrimary else colorOnSurface)
        holder.contentRoot.setCardBackgroundColor(if (item.isSelected) colorPrimary else colorSurface)
        holder.contentRoot.strokeWidth = if (item.isSelected) 0 else dp
        holder.contentRoot.setOnClickListener { actionListener.onDrawerItemClick(item) }
        holder.contentRoot.setOnLongClickListener {
            actionListener.onDrawerItemLongClick(item)
            true
        }
    }

    private fun bindRecentChat(holder: RecentChatHolder, item: RecentChat) {
        holder.tvChatTitle.text = item.title
        holder.tvChatTitle.setTextColor(if (item.isSelected) colorOnPrimary else colorOnSurface)
        if (item.iconUrl.isNullOrEmpty()) {
            with()
                .load(R.drawable.ic_group_chat)
                .transform(transformation)
                .into(holder.ivChatImage)
        } else {
            with()
                .load(item.iconUrl)
                .transform(transformation)
                .into(holder.ivChatImage)
        }
        (holder.contentRoot as MaterialCardView).setCardBackgroundColor(if (item.isSelected) colorPrimary else colorSurface)
        holder.contentRoot.setOnClickListener { actionListener.onDrawerItemClick(item) }
        holder.contentRoot.setOnLongClickListener {
            actionListener.onDrawerItemLongClick(item)
            true
        }
    }

    override fun viewHolder(view: View, type: Int): RecyclerView.ViewHolder {
        when (type) {
            AbsMenuItem.TYPE_DIVIDER -> return DividerHolder(view)
            AbsMenuItem.TYPE_RECENT_CHAT -> return RecentChatHolder(view)
            AbsMenuItem.TYPE_ICON -> return if (paging || !noStroke) NormalHolder(view) else NormalNoStrokeHolder(
                view
            )
            AbsMenuItem.TYPE_WITHOUT_ICON -> return NoIconHolder(view)
        }
        throw IllegalStateException()
    }

    override fun layoutId(type: Int): Int {
        when (type) {
            AbsMenuItem.TYPE_DIVIDER -> return if (noStroke) R.layout.drawer_list_item_divider_no_stroke else R.layout.drawer_list_item_divider
            AbsMenuItem.TYPE_RECENT_CHAT -> return R.layout.item_navigation_recents
            AbsMenuItem.TYPE_ICON -> return if (paging) R.layout.item_navigation else if (noStroke) R.layout.drawer_list_item_no_stroke else R.layout.drawer_list_item
            AbsMenuItem.TYPE_WITHOUT_ICON -> return R.layout.drawer_list_item_without_icon
        }
        throw IllegalStateException()
    }

    override fun getItemType(position: Int): Int {
        return getItem(position - headersCount).type
    }

    interface ActionListener {
        fun onDrawerItemClick(item: AbsMenuItem)
        fun onDrawerItemLongClick(item: AbsMenuItem)
    }

    private class NormalHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.icon)
        val txtTitle: TextView = view.findViewById(R.id.title)
        val tvCount: TextView = view.findViewById(R.id.counter)
        val contentRoot: MaterialCardView = view.findViewById(R.id.content_root)
    }

    private class NormalNoStrokeHolder(view: View) :
        RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.icon)
        val txtTitle: TextView = view.findViewById(R.id.title)
        val tvCount: TextView = view.findViewById(R.id.counter)
        val contentRoot: ViewGroup = view.findViewById(R.id.content_root)
    }

    private class DividerHolder(itemView: View) : RecyclerView.ViewHolder(
        itemView
    )

    private class RecentChatHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val tvChatTitle: TextView = itemView.findViewById(R.id.title)
        val ivChatImage: ImageView = itemView.findViewById(R.id.avatar)
        val contentRoot: View = itemView.findViewById(R.id.content_root)
    }

    private class NoIconHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txTitle: TextView = view.findViewById(R.id.title)
        val contentRoot: View = view.findViewById(R.id.content_root)
    }
}