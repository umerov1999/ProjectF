package dev.ragnarok.fenrir.fragment.products

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Market
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ProductsPresenter(
    accountId: Int,
    private val owner_id: Int,
    private val album_id: Int,
    private val isService: Boolean,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IProductsView>(accountId, savedInstanceState) {
    private val ownerInteractor: IOwnersRepository = owners
    private val mMarkets: ArrayList<Market> = ArrayList()
    private val netDisposable = CompositeDisposable()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(netLoadingNow)
    }

    override fun onDestroyed() {
        netDisposable.dispose()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(ownerInteractor.getMarket(
            accountId,
            owner_id,
            if (album_id == 0) null else album_id,
            offset,
            COUNT_PER_REQUEST,
            isService
        )
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

    override fun onGuiCreated(viewHost: IProductsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(mMarkets)
        resolveRefreshingView()
    }

    private fun canLoadMore(): Boolean {
        return mMarkets.isNotEmpty() && !cacheLoadingNow && !netLoadingNow && !mEndOfContent
    }

    fun fireRefresh() {
        netDisposable.clear()
        netLoadingNow = false
        requestAtLast()
    }

    fun fireMarketOpen(market: Market) {
        view?.onOpenMarket(
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
        requestAtLast()
    }
}