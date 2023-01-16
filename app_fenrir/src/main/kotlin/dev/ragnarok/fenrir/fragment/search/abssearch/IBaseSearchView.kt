package dev.ragnarok.fenrir.fragment.search.abssearch

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.fragment.search.options.BaseOption

interface IBaseSearchView<T> : IMvpView, IErrorView, IToastView,
    IAttachmentsPlacesView {
    fun displayData(data: MutableList<T>)
    fun setEmptyTextVisible(visible: Boolean)
    fun notifyDataSetChanged()
    fun notifyItemChanged(index: Int)
    fun notifyDataAdded(position: Int, count: Int)
    fun showLoading(loading: Boolean)
    fun displayFilter(accountId: Long, options: ArrayList<BaseOption>)
}