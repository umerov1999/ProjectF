package dev.ragnarok.fenrir.fragment.gifts

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
import dev.ragnarok.fenrir.model.Gift
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.picasso.PicassoInstance.Companion.with
import dev.ragnarok.fenrir.util.AppTextUtils
import dev.ragnarok.fenrir.util.ViewUtils.displayAvatar

class GiftAdapter(private var data: List<Gift>, private val context: Context) :
    RecyclerView.Adapter<GiftAdapter.Holder>() {
    private var clickListener: ClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_gift, parent, false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val gift = data[position]
        if (gift.thumb.nonNullNoEmpty()) {
            displayAvatar(holder.thumb, null, gift.thumb, Constants.PICASSO_TAG)
        } else {
            with().cancelRequest(holder.thumb)
        }
        if (gift.thumb.nonNullNoEmpty()) {
            holder.message.visibility = View.VISIBLE
            holder.message.text = gift.message
        } else {
            holder.message.visibility = View.GONE
        }
        if (gift.date == 0L) holder.time.visibility = View.GONE else {
            holder.time.visibility = View.VISIBLE
            holder.time.text = AppTextUtils.getDateFromUnixTime(context, gift.date)
        }
        holder.gift_container.setOnClickListener {
            clickListener?.onOpenClick(holder.bindingAdapterPosition, gift)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(data: List<Gift>) {
        this.data = data
        notifyDataSetChanged()
    }

    fun setClickListener(clickListener: ClickListener?) {
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun onOpenClick(index: Int, gift: Gift)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.item_thumb)
        val message: TextView = itemView.findViewById(R.id.item_message)
        val time: TextView = itemView.findViewById(R.id.item_time)
        val gift_container: View = itemView.findViewById(R.id.gift_container)
    }
}