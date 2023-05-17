package dev.ragnarok.fenrir.fragment.videos.videopreview

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.model.Icon
import dev.ragnarok.fenrir.model.menu.Item
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.view.ColorFilterImageView

class MenuAdapter(context: Context, items: List<Item>, private val big: Boolean) :
    ArrayAdapter<Item>(context, R.layout.item_custom_menu, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        if (convertView != null) {
            view = convertView
        } else {
            view = LayoutInflater.from(parent.context).inflate(
                if (big) R.layout.item_custom_menu_big else R.layout.item_custom_menu,
                parent,
                false
            )
            view.tag = Holder(view)
        }
        val holder = view.tag as Holder
        val item = getItem(position)
        val section = item?.section
        val headerVisible: Boolean = when {
            section == null -> {
                false
            }

            position == 0 -> {
                true
            }

            else -> {
                val previous = getItem(position - 1)
                section != previous?.section
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
        item?.icon?.let { bindIcon(holder.itemIcon, it) }
        item?.color.ifNonNull({
            Utils.setColorFilter(holder.itemIcon, it)
        }, {
            holder.itemIcon.clearColorFilter()
        })
        holder.itemText.text = item?.title?.getText(context)
        val last = position == count - 1
        val dividerVisible: Boolean = if (last) {
            false
        } else {
            val next = getItem(position + 1)
            next?.section != section
        }
        holder.divider.visibility = if (dividerVisible) View.VISIBLE else View.GONE
        return view
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

    internal class Holder(itemView: View) {
        val headerRoot: View = itemView.findViewById(R.id.header_root)
        val headerIcon: ImageView = itemView.findViewById(R.id.header_icon)
        val headerText: TextView = itemView.findViewById(R.id.header_text)
        val itemOffsetView: View = itemView.findViewById(R.id.item_offset)
        val itemIcon: ColorFilterImageView = itemView.findViewById(R.id.item_icon)
        val itemText: TextView = itemView.findViewById(R.id.item_text)
        val divider: View = itemView.findViewById(R.id.divider)
    }
}