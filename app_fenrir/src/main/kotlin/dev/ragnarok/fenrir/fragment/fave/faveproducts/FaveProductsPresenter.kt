package dev.ragnarok.fenrir.fragment.fave.faveproducts

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class FaveProductsPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFaveProductsView>(accountId, savedInstanceState) {
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val mMarkets: ArrayList<Market> = ArrayList()
    private val cacheDisposable = CompositeJob()
    private val netDisposable = CompositeJob()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private var doLoadTabs = false
    private var offsetPos = 0
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        requestAtLast()
    }

    private fun loadCachedData() {
        cacheLoadingNow = true
        cacheDisposable.add(
            faveInteractor.getCachedProducts(accountId)
                .fromIOToMain({ markets -> onCachedDataReceived(markets) }) { t ->
                    onCacheGetError(
                        t
                    )
                })
    }

    private fun onCacheGetError(t: Throwable) {
        cacheLoadingNow = false
        showError(t)
    }

    private fun onCachedDataReceived(markets: List<Market>) {
        cacheLoadingNow = false
        mMarkets.clear()
        mMarkets.addAll(markets)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        netDisposable.cancel()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(
            faveInteractor.getProducts(accountId, COUNT, offset)
                .fromIOToMain({ products ->
                    onNetDataReceived(
                        offset,
                        products
                    )
                }) { t -> onNetDataGetError(t) })
    }

    private fun onNetDataGetError(t: Throwable) {
        netLoadingNow = false
        resolveRefreshingView()
        showError(t)
    }

    private fun onNetDataReceived(offset: Int, markets: List<Market>) {
        cacheDisposable.clear()
        cacheLoadingNow = false
        mEndOfContent = markets.isEmpty()
        netLoadingNow = false
        offsetPos += COUNT
        if (offset == 0) {
            mMarkets.clear()
            mMarkets.addAll(markets)
            view?.notifyDataSetChanged()
        } else {
            val tmp = Utils.stripEqualsWithCounter(markets, mMarkets, COUNT)
            if (tmp.isEmpty()) {
                mEndOfContent = true
            } else {
                val startSize = mMarkets.size
                mMarkets.addAll(tmp)
                view?.notifyDataAdded(
                    startSize,
                    tmp.size
                )
            }
        }
        resolveRefreshingView()
    }

    private fun requestAtLast() {
        offsetPos = 0
        request(0)
    }

    private fun requestNext() {
        request(offsetPos)
    }

    override fun onGuiCreated(viewHost: IFaveProductsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mMarkets)
    }

    private fun canLoadMore(): Boolean {
        return mMarkets.isNotEmpty() && !cacheLoadingNow && !netLoadingNow && !mEndOfContent
    }

    fun fireRefresh() {
        cacheDisposable.clear()
        netDisposable.clear()
        netLoadingNow = false
        requestAtLast()
    }

    fun fireMarketOpen(market: Market) {
        view?.onMarketOpen(
            accountId,
            market
        )
    }

    fun fireScrollToEnd() {
        if (canLoadMore()) {
            requestNext()
        }
    }

    companion object {
        private const val COUNT = 25
    }

    init {
        loadCachedData()
    }
}