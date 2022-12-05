package dev.ragnarok.fenrir.fragment.narratives

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Narratives
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.picasso.PicassoInstance
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class NarrativesAdapter(private var data: List<Narratives>, private val context: Context) :
    RecyclerView.Adapter<NarrativesAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(context).inflate(R.layout.item_narratives, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val narrative = data[position]
        if (narrative.cover.nonNullNoEmpty()) {
            holder.thumb.visibility = View.VISIBLE
            displayAvatar(holder.thumb, null, narrative.cover, Constants.PICASSO_TAG)
        } else {
            holder.thumb.visibility = View.GONE
            PicassoInstance.with().cancelRequest(holder.thumb)
        }
        holder.title.text = narrative.title
        if (narrative.stories?.size.orZero() <= 0) holder.count.visibility = View.GONE else {
            holder.count.visibility = View.VISIBLE
            holder.count.text =
                context.getString(R.string.narratives_count, narrative.stories?.size.orZero())
        }
        holder.itemView.setOnClickListener {
            clickListener?.onOpenClick(holder.bindingAdapterPosition, narrative)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Narratives>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onOpenClick(index: Int, narratives: Narratives)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.item_thumb)
        val title: TextView = itemView.findViewById(R.id.item_title)
        val count: TextView = itemView.findViewById(R.id.item_count)
    }
}
