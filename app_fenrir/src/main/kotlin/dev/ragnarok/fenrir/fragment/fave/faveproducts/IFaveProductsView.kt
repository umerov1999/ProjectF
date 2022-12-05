package dev.ragnarok.fenrir.fragment.fave.faveproducts

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Market

interface IFaveProductsView : IMvpView, IErrorView {
    fun displayData(markets: List<Market>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun onMarketOpen(accountId: Int, market: Market)
}