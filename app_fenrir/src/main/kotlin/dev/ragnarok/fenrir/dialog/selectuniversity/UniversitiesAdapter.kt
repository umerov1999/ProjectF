package dev.ragnarok.fenrir.dialog.selectuniversity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.database.University

class UniversitiesAdapter(private val mContext: Context, private val mData: List<University>) :
    RecyclerView.Adapter<UniversitiesAdapter.Holder>() {
    private var mListener: Listener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                mContext
            ).inflate(R.layout.item_country, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val country = mData[position]
        holder.name.text = country.title
        holder.itemView.setOnClickListener {
            mListener?.onClick(country)
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    interface Listener {
        fun onClick(university: University)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
    }
}