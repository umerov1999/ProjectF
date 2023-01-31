package dev.ragnarok.fenrir.fragment.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerMenuAdapter.MenuItemHolder
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.menu.AdvancedItem
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.view.ColorFilterImageView

class RecyclerMenuAdapter : RecyclerView.Adapter<MenuItemHolder> {
    @LayoutRes
    private val itemRes: Int
    private var items: List<AdvancedItem>
    private var actionListener: ActionListener? = null

    constructor(@LayoutRes itemLayout: Int, items: List<AdvancedItem>) {
        itemRes = itemLayout
        this.items = items
    }

    constructor(items: List<AdvancedItem>) {
        this.items = items
        itemRes = R.layout.item_advanced_menu
    }

    fun setItems(items: List<AdvancedItem>) {
        this.items = items
        notifyDataSetChanged()
    }

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemHolder {
        return MenuItemHolder(LayoutInflater.from(parent.context).inflate(itemRes, parent, false))
    }

    override fun onBindViewHolder(holder: MenuItemHolder, position: Int) {
        onBindMenuItemHolder(holder, position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun getItem(position: Int): AdvancedItem {
        return items[position]
    }

    private fun onBindMenuItemHolder(holder: MenuItemHolder, position: Int) {
        val context = holder.itemView.context
        val item = getItem(position)
        val section = item.section
        val headerVisible: Boolean = when {
            section == null -> {
                false
            }

            position == 0 -> {
                true
            }

            else -> {
                val previous = getItem(position - 1)
                section != previous.section
            }
        }
        holder.headerRoot.setOnClickListener { }
        if (headerVisible) {
            holder.headerRoot.visibility = View.VISIBLE
            holder.headerText.text = section?.title?.getText(context)
            section?.icon.ifNonNull({
                holder.headerIcon.visibility = View.VISIBLE
                holder.headerIcon.setImageResource(it)
            }, {
                holder.headerIcon.visibility = View.GONE
            })
        } else {
            holder.headerRoot.visibility = View.GONE
        }
        holder.itemOffsetView.visibility = if (section != null) View.VISIBLE else View.GONE
        item.icon?.let { bindIcon(holder.itemIcon, it) }
        holder.itemTitle.text = item.title?.getText(context)
        holder.itemSubtitle.visibility =
            if (item.subtitle == null) View.GONE else View.VISIBLE
        holder.itemSubtitle.text = item.subtitle?.getText(context)
        val last = position == itemCount - 1
        val dividerVisible: Boolean = if (last) {
            false
        } else {
            val next = getItem(position + 1)
            next.section != section
        }
        holder.divider.visibility = if (dividerVisible) View.VISIBLE else View.GONE
        holder.itemRoot.setOnClickListener {
            actionListener?.onClick(item)
        }
        holder.itemRoot.setOnLongClickListener {
            if (actionListener != null) {
                actionListener?.onLongClick(item)
                return@setOnLongClickListener true
            }
            false
        }
    }

    private fun bindIcon(imageView: ColorFilterImageView, icon: Icon) {
        imageView.visibility = View.VISIBLE
        if (icon.isRemote) {
            imageView.setColorFilterEnabled(false)
            with()
                .load(icon.url)
                .transform(CurrentTheme.createTransformationForAvatar())
                .into(imageView)
        } else {
            imageView.setColorFilterEnabled(true)
            with().cancelRequest(imageView)
            icon.res?.let { imageView.setImageResource(it) }
        }
    }

    interface ActionListener {
        fun onClick(item: AdvancedItem)
        fun onLongClick(item: AdvancedItem)
    }

    class MenuItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerRoot: View = itemView.findViewById(R.id.header_root)
        val headerIcon: ImageView = itemView.findViewById(R.id.header_icon)
        val headerText: TextView = itemView.findViewById(R.id.header_text)
        val itemOffsetView: View = itemView.findViewById(R.id.item_offset)
        val itemIcon: ColorFilterImageView = itemView.findViewById(R.id.item_icon)
        val itemTitle: TextView = itemView.findViewById(R.id.item_title)
        val itemSubtitle: TextView = itemView.findViewById(R.id.item_subtitle)
        val itemRoot: View = itemView.findViewById(R.id.item_root)
        val divider: View = itemView.findViewById(R.id.divider)
    }
}