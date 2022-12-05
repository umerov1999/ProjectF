package dev.ragnarok.fenrir.fragment.products

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Market

interface IProductsView : IMvpView, IErrorView {
    fun displayData(market: List<Market>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun onOpenMarket(accountId: Int, market: Market)
}