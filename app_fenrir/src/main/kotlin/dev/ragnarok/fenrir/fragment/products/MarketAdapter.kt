package dev.ragnarok.fenrir.fragment.products

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class MarketAdapter(private var data: List<Market>, private val context: Context) :
    RecyclerView.Adapter<MarketAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_market, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val market = data[position]
        if (market.thumb_photo.nonNullNoEmpty()) displayAvatar(
            holder.thumb,
            null,
            market.thumb_photo,
            Constants.PICASSO_TAG
        ) else holder.thumb.setImageResource(R.drawable.ic_market_colored_outline)
        holder.title.text = market.title
        if (market.description.isNullOrEmpty()) holder.description.visibility = View.GONE else {
            holder.description.visibility = View.VISIBLE
            holder.description.text = market.description
        }
        if (market.price.isNullOrEmpty()) holder.price.visibility = View.GONE else {
            holder.price.visibility = View.VISIBLE
            holder.price.text = market.price
        }
        if (market.date == 0L) holder.time.visibility = View.GONE else {
            holder.time.visibility = View.VISIBLE
            holder.time.text = AppTextUtils.getDateFromUnixTime(context, market.date)
        }
        when (market.availability) {
            0 -> {
                holder.available.setTextColor(CurrentTheme.getColorOnSurface(context))
                holder.available.setText(R.string.markets_available)
            }
            2 -> {
                holder.available.setTextColor(Color.parseColor("#ffaa00"))
                holder.available.setText(R.string.markets_not_available)
            }
            else -> {
                holder.available.setTextColor(Color.parseColor("#ff0000"))
                holder.available.setText(R.string.markets_deleted)
            }
        }
        holder.market_container.setOnClickListener {
            clickListener?.onOpenClick(holder.bindingAdapterPosition, market)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Market>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onOpenClick(index: Int, market: Market)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.item_thumb)
        val price: TextView = itemView.findViewById(R.id.item_price)
        val title: TextView = itemView.findViewById(R.id.item_title)
        val description: TextView = itemView.findViewById(R.id.item_description)
        val available: TextView = itemView.findViewById(R.id.item_available)
        val time: TextView = itemView.findViewById(R.id.item_time)
        val market_container: View = itemView.findViewById(R.id.market_container)
    }
}