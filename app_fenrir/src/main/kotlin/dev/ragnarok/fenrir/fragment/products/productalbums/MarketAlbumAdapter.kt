package dev.ragnarok.fenrir.fragment.products.productalbums

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
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.model.PhotoSize
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class MarketAlbumAdapter(private var data: List<MarketAlbum>, private val context: Context) :
    RecyclerView.Adapter<MarketAlbumAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(context).inflate(R.layout.item_market_album, parent, false)
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val market = data[position]
        if (market.getPhoto() != null) {
            val url = market.getPhoto()?.getUrlForSize(PhotoSize.X, true)
            displayAvatar(holder.thumb, null, url, Constants.PICASSO_TAG)
        } else holder.thumb.setImageResource(R.drawable.ic_market_colored_stack)
        holder.title.text = market.getTitle()
        if (market.getCount() == 0) holder.count.visibility = View.GONE else {
            holder.count.visibility = View.VISIBLE
            holder.count.text = context.getString(R.string.markets_count, market.getCount())
        }
        if (market.getUpdated_time() == 0L) holder.time.visibility = View.GONE else {
            holder.time.visibility = View.VISIBLE
            holder.time.text =
                AppTextUtils.getDateFromUnixTime(context, market.getUpdated_time())
        }
        holder.market_container.setOnClickListener {
            clickListener?.onOpenClick(holder.bindingAdapterPosition, market)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<MarketAlbum>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onOpenClick(index: Int, market_album: MarketAlbum)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.item_thumb)
        val title: TextView = itemView.findViewById(R.id.item_title)
        val count: TextView = itemView.findViewById(R.id.item_count)
        val time: TextView = itemView.findViewById(R.id.item_time)
        val market_container: View = itemView.findViewById(R.id.market_container)
    }
}