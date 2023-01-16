package dev.ragnarok.fenrir.fragment.marketview

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.Peer

interface IMarketViewView : IMvpView, IErrorView {
    fun displayLoading(loading: Boolean)
    fun displayMarket(market: Market, accountId: Long)
    fun sendMarket(accountId: Long, market: Market)
    fun onWriteToMarketer(accountId: Long, market: Market, peer: Peer)
}