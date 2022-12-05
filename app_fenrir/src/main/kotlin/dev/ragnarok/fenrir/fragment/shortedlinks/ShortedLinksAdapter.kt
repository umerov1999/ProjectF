package dev.ragnarok.fenrir.fragment.shortedlinks

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.ShortLink
import dev.ragnarok.fenrir.util.AppTextUtils

class ShortedLinksAdapter(private var data: List<ShortLink>, private val context: Context) :
    RecyclerView.Adapter<ShortedLinksAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_short_link, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val link = data[position]
        holder.time.text = AppTextUtils.getDateFromUnixTime(context, link.timestamp)
        holder.views.text = link.views.toString()
        holder.short_link.text = link.short_url
        holder.original.text = link.url
        holder.delete.setOnClickListener {
            clickListener?.onDelete(holder.bindingAdapterPosition, link)
        }
        holder.copy.setOnClickListener {
            clickListener?.onCopy(holder.bindingAdapterPosition, link)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<ShortLink>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onCopy(index: Int, link: ShortLink)
        fun onDelete(index: Int, link: ShortLink)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val short_link: TextView = itemView.findViewById(R.id.item_short_link)
        val original: TextView = itemView.findViewById(R.id.item_link)
        val time: TextView = itemView.findViewById(R.id.item_time)
        val views: TextView = itemView.findViewById(R.id.item_views)
        val copy: ImageView = itemView.findViewById(R.id.item_copy)
        val delete: ImageView = itemView.findViewById(R.id.item_delete)
    }
}