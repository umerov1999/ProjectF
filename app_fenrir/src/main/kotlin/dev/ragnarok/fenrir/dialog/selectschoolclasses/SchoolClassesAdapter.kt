package dev.ragnarok.fenrir.dialog.selectschoolclasses

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.database.SchoolClazz

class SchoolClassesAdapter(private val mContext: Context, private val mData: List<SchoolClazz>) :
    RecyclerView.Adapter<SchoolClassesAdapter.Holder>() {
    private var mListener: Listener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(
                mContext
            ).inflate(R.layout.item_country, parent, false)
        )
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val schoolClazzDto = mData[position]
        holder.name.text = schoolClazzDto.title
        holder.itemView.setOnClickListener {
            mListener?.onClick(schoolClazzDto)
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setListener(listener: Listener?) {
        mListener = listener
    }

    interface Listener {
        fun onClick(schoolClazz: SchoolClazz)
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.name)
    }
}