package dev.ragnarok.fenrir.fragment.fave.faveproducts

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Market
import io.reactivex.rxjava3.disposables.CompositeDisposable

class FaveProductsPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFaveProductsView>(accountId, savedInstanceState) {
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val mMarkets: ArrayList<Market> = ArrayList()
    private val cacheDisposable = CompositeDisposable()
    private val netDisposable = CompositeDisposable()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    public override fun onGuiResumed() {
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
        cacheDisposable.add(faveInteractor.getCachedProducts(accountId)
            .fromIOToMain()
            .subscribe({ markets -> onCachedDataReceived(markets) }) { t ->
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
        cacheDisposable.dispose()
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(faveInteractor.getProducts(accountId, COUNT_PER_REQUEST, offset)
            .fromIOToMain()
            .subscribe({ products ->
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
        if (offset == 0) {
            mMarkets.clear()
            mMarkets.addAll(markets)
            view?.notifyDataSetChanged()
        } else {
            val startSize = mMarkets.size
            mMarkets.addAll(markets)
            view?.notifyDataAdded(
                startSize,
                markets.size
            )
        }
        resolveRefreshingView()
    }

    private fun requestAtLast() {
        request(0)
    }

    private fun requestNext() {
        request(mMarkets.size)
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
        private const val COUNT_PER_REQUEST = 25
    }

    init {
        loadCachedData()
    }
}