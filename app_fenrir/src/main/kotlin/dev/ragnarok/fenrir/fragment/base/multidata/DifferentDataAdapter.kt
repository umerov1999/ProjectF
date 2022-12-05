package dev.ragnarok.fenrir.fragment.base.multidata

import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.util.Utils

abstract class DifferentDataAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mData: ArrayList<List<*>> = ArrayList(2)
    override fun getItemCount(): Int {
        var count = 0
        for (data in mData) {
            count += data.size
        }
        return count
    }

    fun setData(type: Int, data: List<*>) {
        mData.add(type, data)
    }

    fun notifyItemChanged(dataPosition: Int, dataType: Int) {
        notifyItemChanged(getOffset(dataType) + dataPosition)
    }

    fun notifyItemRangeInserted(dataPositionStart: Int, count: Int, dataType: Int) {
        notifyItemRangeInserted(getOffset(dataType) + dataPositionStart, count)
    }

    fun notifyItemInserted(dataPosition: Int, dataType: Int) {
        notifyItemInserted(dataPosition + getOffset(dataType))
    }

    fun notifyItemRemoved(dataPosition: Int, dataType: Int) {
        notifyItemRemoved(dataToAdapterPosition(dataPosition, dataType))
    }

    private fun dataToAdapterPosition(dataPosition: Int, dataType: Int): Int {
        return dataPosition + getOffset(dataType)
    }

    private fun getOffset(type: Int): Int {
        var offset = 0
        for (i in mData.indices) {
            if (i < type) {
                offset += Utils.safeCountOf(mData[i])
            }
        }
        return offset
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getItem(adapterPosition: Int): T {
        var offset = 0
        for (data in mData) {
            val newOffset = offset + data.size
            if (adapterPosition < newOffset) {
                val internalPosition = adapterPosition - offset
                return data[internalPosition] as T
            }
            offset = newOffset
        }
        throw IllegalArgumentException("Invalid adapter position")
    }

    protected fun getDataTypeByAdapterPosition(adapterPosition: Int): Int {
        var offset = 0
        for (i in mData.indices) {
            val data = mData[i]
            val newOffset = offset + data.size
            if (adapterPosition < newOffset) {
                return i
            }
            offset = newOffset
        }
        throw IllegalArgumentException("Invalid adapter position")
    }

}