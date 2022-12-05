package dev.ragnarok.fenrir.fragment.base.multidata

import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import dev.ragnarok.fenrir.model.DataWrapper

abstract class MultyDataAdapter<T, VH : RecyclerView.ViewHolder>(
    private var fullData: List<DataWrapper<T>>,
    @StringRes private var titles: Array<Int?>
) : RecyclerView.Adapter<VH>() {
    fun setData(wrappers: List<DataWrapper<T>>, titles: Array<Int?>) {
        fullData = wrappers
        this.titles = titles
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        var count = 0
        for (pair in fullData) {
            if (!pair.isEnabled) {
                continue
            }
            count += pair.size()
        }
        return count
    }

    protected operator fun get(adapterPosition: Int, info: ItemInfo<T>) {
        var offset = 0
        for (i in fullData.indices) {
            val wrapper = fullData[i]
            if (!wrapper.isEnabled) {
                continue
            }
            val newOffset = offset + wrapper.size()
            if (adapterPosition < newOffset) {
                val internalPosition = adapterPosition - offset
                info.item = wrapper.get()[internalPosition]
                info.internalPosition = internalPosition
                info.sectionTitleRes = titles[i]
                return
            }
            offset = newOffset
        }
        throw IllegalArgumentException("Invalid adapter position")
    }

    fun notifyItemRangeInserted(dataIndex: Int, internalPosition: Int, count: Int) {
        notifyItemRangeInserted(getAdapterPosition(dataIndex, internalPosition), count)
    }

    fun notifyItemRemoved(dataIndex: Int, internalPosition: Int) {
        notifyItemRemoved(getAdapterPosition(dataIndex, internalPosition))
    }

    fun notifyItemChanged(dataIndex: Int, internalPosition: Int) {
        notifyItemChanged(getAdapterPosition(dataIndex, internalPosition))
    }

    private fun getAdapterPosition(dataIndex: Int, internalPosition: Int): Int {
        var offset = 0
        for (i in fullData.indices) {
            offset = if (i < dataIndex) {
                offset + fullData[i].size()
            } else {
                break
            }
        }
        return offset + internalPosition
    }

    protected operator fun get(adapterPosition: Int): ItemInfo<T> {
        val info = ItemInfo<T>()
        get(adapterPosition, info)
        return info
    }

    fun getItemAt(adapterPosition: Int): T {
        var offset = 0
        for (dataWrapper in fullData) {
            if (!dataWrapper.isEnabled) {
                continue
            }
            val newOffset = offset + dataWrapper.size()
            if (adapterPosition < newOffset) {
                val internalPosition = adapterPosition - offset
                return dataWrapper.get()[internalPosition]
            }
            offset = newOffset
        }
        throw IllegalArgumentException("Invalid position")
    }

    class ItemInfo<T> {
        var item: T? = null
        var internalPosition = 0
        var sectionTitleRes: Int? = null
    }
}