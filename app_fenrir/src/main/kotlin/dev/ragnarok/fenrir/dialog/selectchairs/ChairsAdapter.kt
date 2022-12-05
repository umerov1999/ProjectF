package dev.ragnarok.fenrir.dialog.selectchairs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.database.Chair

class ChairsAdapter(private val mContext: Context, private val mData: List<Chair>) :
    RecyclerView.Adapter<ChairsAdapter.Holder>() {
    private var mListener: Listener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                mContext
            ).inflate(R.layout.item_country, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chair = mData[position]
        holder.name.text = chair.title
        holder.itemView.setOnClickListener {
            mListener?.onClick(chair)
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    interface Listener {
        fun onClick(chair: Chair)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)

    }
}