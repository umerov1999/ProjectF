package dev.ragnarok.fenrir.fragment.audio.catalog_v2.sections

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.LoadMoreState

interface ICatalogV2SectionView : IMvpView, IErrorView, IToastView {
    fun displayData(pages: MutableList<AbsModel>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDataChanged(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun setupLoadMoreFooter(@LoadMoreState state: Int)
    fun updateLayoutManager(type: String)
}
