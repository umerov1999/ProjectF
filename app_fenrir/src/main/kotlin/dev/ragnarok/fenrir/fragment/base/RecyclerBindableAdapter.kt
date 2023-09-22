package dev.ragnarok.fenrir.fragment.base

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlin.math.abs

abstract class RecyclerBindableAdapter<T, VH : RecyclerView.ViewHolder>(private var items: MutableList<T>) :
    AbsRecyclerViewAdapter<VH>() {
    private val headers: MutableList<View> = ArrayList()
    private val footers: MutableList<View> = ArrayList()
    private var manager: RecyclerView.LayoutManager? = null
    private val spanSizeLookup: SpanSizeLookup = object : SpanSizeLookup() {
        override fun getSpanSize(position: Int): Int {
            return getGridSpan(position)
        }
    }
    private var inflater: LayoutInflater? = null
    val realItemCount: Int
        get() = items.size

    fun getItem(position: Int): T {
        return items[position]
    }

    fun getItemRawPosition(position: Int): Int {
        return position - headers.size
    }

    fun setItems(items: MutableList<T>?, notifyDatasetChanged: Boolean) {
        if (items == null) {
            this.items = mutableListOf()
        } else {
            this.items = items
        }
        if (notifyDatasetChanged) {
            notifyDataSetChanged()
        }
    }

    fun add(position: Int, item: T) {
        items.add(position, item)
        notifyItemInserted(position)
        val positionStart = position + headersCount
        val itemCount = items.size - position
        notifyItemRangeChanged(positionStart, itemCount)
    }

    fun add(item: T) {
        items.add(item)
        notifyItemInserted(items.size - 1 + headersCount)
    }

    fun addAll(items: List<T>) {
        val size = this.items.size
        this.items.addAll(items)
        notifyItemRangeInserted(size + headersCount, items.size)
    }

    fun notifyItemBindableMoved(fromPosition: Int, toPosition: Int) {
        notifyItemMoved(fromPosition + headersCount, toPosition + headersCount)
    }

    fun notifyItemBindableChanged(position: Int) {
        notifyItemChanged(position + headersCount)
    }

    fun notifyItemBindableRemoved(position: Int) {
        notifyItemRemoved(position + headersCount)
        val positionStart = position + headersCount
        val itemCount = items.size - position
        notifyItemRangeChanged(positionStart, itemCount)
    }

    fun notifyItemBindableRangeRemoved(position: Int, count: Int) {
        notifyItemRangeRemoved(position + headersCount, count)
    }

    fun notifyItemBindableRangeInserted(position: Int, count: Int) {
        notifyItemRangeInserted(position + headersCount, count)
    }

    fun notifyItemBindableRangeChanged(position: Int, count: Int) {
        notifyItemRangeChanged(position + headersCount, count)
    }

    fun addAll(position: Int, items: List<T>) {
        val size = this.items.size
        this.items.addAll(position, items)
        notifyItemRangeInserted(position + headersCount + size, items.size - position)
    }

    operator fun set(position: Int, item: T) {
        items[position] = item
        notifyItemChanged(position + headersCount)
    }

    fun removeChild(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position + headersCount)
        val positionStart = position + headersCount
        val itemCount = items.size - position
        notifyItemRangeChanged(positionStart, itemCount)
    }

    fun clear() {
        val size = items.size
        items.clear()
        notifyItemRangeRemoved(headersCount, size)
    }

    fun moveChildTo(fromPosition: Int, toPosition: Int) {
        if (toPosition != -1 && toPosition < items.size) {
            val item = items.removeAt(fromPosition)
            items.add(toPosition, item)
            notifyItemMoved(headersCount + fromPosition, headersCount + toPosition)
            val positionStart = fromPosition.coerceAtMost(toPosition)
            val itemCount = abs(fromPosition - toPosition) + 1
            notifyItemRangeChanged(positionStart + headersCount, itemCount)
        }
    }

    fun indexOf(obj: T): Int {
        return items.indexOf(obj)
    }

    fun indexOfAdapter(obj: T): Int {
        val ret = items.indexOf(obj)
        return if (ret == -1) {
            ret
        } else {
            ret + headers.size
        }
    }

    fun getItems(): MutableList<T> {
        return items
    }

    fun setItems(items: MutableList<T>) {
        setItems(items, true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): VH {
        //if our position is one of our items (this comes from getItemViewType(int position) below)
        return if (type != TYPE_HEADER && type != TYPE_FOOTER) {
            onCreateItemViewHolder(viewGroup, type)
            //else we have a header/footer
        } else {
            //create a new framelayout, or inflate from a resource
            val frameLayout = FrameLayout(viewGroup.context)
            //make sure it fills the space
            frameLayout.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            HeaderFooterViewHolder(frameLayout) as VH
        }
    }

    override fun onBindViewHolder(vh: VH, position: Int) {
        //check what type of view our position is
        when {
            isHeader(position) -> {
                val v = headers[position]
                //add our view to a header view and display it
                prepareHeaderFooter(vh as HeaderFooterViewHolder, v)
            }

            isFooter(position) -> {
                val v = footers[position - realItemCount - headersCount]
                //add our view to a footer view and display it
                prepareHeaderFooter(vh as HeaderFooterViewHolder, v)
            }

            else -> {
                //it's one of our items, display as required
                onBindItemViewHolder(vh, position - headers.size, getItemType(position))
            }
        }
    }

    private fun prepareHeaderFooter(vh: HeaderFooterViewHolder, view: View) {
        //if it's a staggered grid, span the whole layout
        if (manager is StaggeredGridLayoutManager) {
            val layoutParams = StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.isFullSpan = true
            vh.itemView.layoutParams = layoutParams
        }

        //if the view already belongs to another layout, remove it
        if (view.parent != null) {
            (view.parent as ViewGroup).removeView(view)
        }

        //empty out our FrameLayout and replace with our header/footer
        (vh.itemView as ViewGroup).let {
            if (it.childCount > 0) {
                it.removeAllViews()
            }
            it.addView(view)
        }
    }

    private fun isHeader(position: Int): Boolean {
        return position < headers.size
    }

    private fun isFooter(position: Int): Boolean {
        return footers.size > 0 && position >= headersCount + realItemCount
    }

    private fun onCreateItemViewHolder(parent: ViewGroup?, type: Int): VH {
        return viewHolder(inflater?.inflate(layoutId(type), parent, false)!!, type)
    }

    override fun getItemCount(): Int {
        return headers.size + realItemCount + footers.size
    }

    override fun getItemViewType(position: Int): Int {
        //check what type our position is, based on the assumption that the order is headers > items > footers
        if (isHeader(position)) {
            return TYPE_HEADER
        } else if (isFooter(position)) {
            return TYPE_FOOTER
        }
        val type = getItemType(position)
        require(!(type == TYPE_HEADER || type == TYPE_FOOTER)) { "Item type cannot equal $TYPE_HEADER or $TYPE_FOOTER" }
        return type
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        if (manager == null) {
            setManager(recyclerView.layoutManager)
        }
        if (inflater == null) {
            inflater = LayoutInflater.from(recyclerView.context)
        }
    }

    private fun setManager(manager: RecyclerView.LayoutManager?) {
        this.manager = manager
        if (this.manager is GridLayoutManager) {
            (this.manager as GridLayoutManager?)?.spanSizeLookup = spanSizeLookup
        } else if (this.manager is StaggeredGridLayoutManager) {
            (this.manager as StaggeredGridLayoutManager?)?.gapStrategy =
                StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
    }

    protected fun getGridSpan(position: Int): Int {
        var tPosition = position
        if (isHeader(tPosition) || isFooter(tPosition)) {
            return maxGridSpan
        }
        tPosition -= headers.size
        return if (getItem(tPosition) is SpanItemInterface) {
            (getItem(tPosition) as SpanItemInterface).gridSpan
        } else 1
    }

    private val maxGridSpan: Int
        get() {
            if (manager is GridLayoutManager) {
                return (manager as GridLayoutManager).spanCount
            } else if (manager is StaggeredGridLayoutManager) {
                return (manager as StaggeredGridLayoutManager).spanCount
            }
            return 1
        }

    //add a header to the mAdapter
    fun addHeader(header: View) {
        if (!headers.contains(header)) {
            headers.add(header)
            //animate
            notifyItemInserted(headers.size - 1)
        }
    }

    //remove header from mAdapter
    fun removeHeader(header: View) {
        if (headers.contains(header)) {

            //animate
            notifyItemRemoved(headers.indexOf(header))
            headers.remove(header)
        }
    }

    //add a footer to the mAdapter
    fun addFooter(footer: View) {
        if (!footers.contains(footer)) {
            footers.add(footer)

            //animate
            notifyItemInserted(headers.size + itemCount + footers.size - 1)
        }
    }

    //remove footer from mAdapter
    fun removeFooter(footer: View) {
        if (footers.contains(footer)) {
            //animate
            notifyItemRemoved(headers.size + itemCount + footers.indexOf(footer))
            footers.remove(footer)
        }
    }

    val headersCount: Int
        get() = headers.size

    fun getHeader(location: Int): View {
        return headers[location]
    }

    val footersCount: Int
        get() = footers.size

    fun getFooter(location: Int): View {
        return footers[location]
    }

    protected open fun getItemType(position: Int): Int {
        return 0
    }

    protected abstract fun onBindItemViewHolder(viewHolder: VH, position: Int, type: Int)
    protected abstract fun viewHolder(view: View, type: Int): VH

    @LayoutRes
    protected abstract fun layoutId(type: Int): Int
    interface SpanItemInterface {
        val gridSpan: Int
    }

    //our header/footer RecyclerView.ViewHolder is just a FrameLayout
    private class HeaderFooterViewHolder(itemView: View) : RecyclerView.ViewHolder(
        itemView
    )

    companion object {
        const val TYPE_HEADER = 7898
        const val TYPE_FOOTER = 7899
    }
}