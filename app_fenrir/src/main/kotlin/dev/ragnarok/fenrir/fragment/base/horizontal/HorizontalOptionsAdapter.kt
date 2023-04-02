package dev.ragnarok.fenrir.fragment.base.horizontal

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.fragment.base.RecyclerBindableAdapter
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils

class HorizontalOptionsAdapter<T : Entry>(data: MutableList<T>) :
    RecyclerBindableAdapter<T, HorizontalOptionsAdapter.Holder>(data) {
    private var listener: Listener<T>? = null
    private var delete_listener: CustomListener<T>? = null
    override fun onBindItemViewHolder(viewHolder: Holder, position: Int, type: Int) {
        val item: T = getItem(position)
        val title = item.getTitle(viewHolder.itemView.context)
        val targetTitle =
            when {
                (title
                    ?: return).startsWith("#") -> title

                item.isCustom -> title
                else -> "#$title"
            }
        val context = viewHolder.itemView.context
        viewHolder.title.text = targetTitle
        viewHolder.title.setTextColor(
            if (item.isActive) CurrentTheme.getColorOnPrimary(context) else CurrentTheme.getPrimaryTextColorCode(
                context
            )
        )
        viewHolder.background.setCardBackgroundColor(
            if (item.isActive) CurrentTheme.getColorPrimary(
                context
            ) else CurrentTheme.getColorSurface(context)
        )
        viewHolder.background.strokeWidth = if (item.isActive) 0 else Utils.dpToPx(1f, context)
            .toInt()
        viewHolder.itemView.setOnClickListener { listener?.onOptionClick(item) }
        viewHolder.delete.setColorFilter(
            if (item.isActive) CurrentTheme.getColorOnPrimary(context) else CurrentTheme.getPrimaryTextColorCode(
                context
            )
        )
        viewHolder.delete.visibility = if (item.isCustom) View.VISIBLE else View.GONE
        viewHolder.delete.setOnClickListener {
            if (item.isCustom) {
                delete_listener?.onDeleteOptionClick(
                    item,
                    getItemRawPosition(viewHolder.bindingAdapterPosition)
                )
            }
        }
    }

    override fun viewHolder(view: View, type: Int): Holder {
        return Holder(view)
    }

    override fun layoutId(type: Int): Int {
        return R.layout.item_chip
    }

    fun setListener(listener: Listener<T>?) {
        this.listener = listener
    }

    fun setDeleteListener(listener: CustomListener<T>?) {
        delete_listener = listener
    }

    interface Listener<T : Entry> {
        fun onOptionClick(entry: T)
    }

    interface CustomListener<T : Entry> {
        fun onDeleteOptionClick(entry: T, position: Int)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val background: MaterialCardView = itemView.findViewById(R.id.card_view)
        val title: TextView = itemView.findViewById(R.id.title)
        val delete: ImageView = itemView.findViewById(R.id.delete)
    }
}