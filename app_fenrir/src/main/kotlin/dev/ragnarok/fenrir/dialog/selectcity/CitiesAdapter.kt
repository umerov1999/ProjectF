package dev.ragnarok.fenrir.dialog.selectcity

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.City

class CitiesAdapter(private val mContext: Context, private val mData: List<City>) :
    RecyclerView.Adapter<CitiesAdapter.Holder>() {
    private var mListener: Listener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                mContext
            ).inflate(R.layout.item_city, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val city = mData[position]
        holder.title.text = city.title
        holder.title.setTypeface(null, if (city.isImportant) Typeface.BOLD else Typeface.NORMAL)
        holder.region.text = city.region
        holder.region.visibility = if (city.region.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.area.text = city.area
        holder.area.visibility = if (city.area.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener {
            mListener?.onClick(city)
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    interface Listener {
        fun onClick(city: City)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val area: TextView = itemView.findViewById(R.id.area)
        val region: TextView = itemView.findViewById(R.id.region)
    }
}