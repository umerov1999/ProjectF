package dev.ragnarok.fenrir.fragment.products.productalbums

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.MarketAlbum
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class ProductAlbumsPresenter(
    accountId: Long,
    private val owner_id: Long,
    private val allProductString: String?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IProductAlbumsView>(accountId, savedInstanceState) {
    private val ownerInteractor: IOwnersRepository = owners
    private val mMarkets: ArrayList<MarketAlbum> = ArrayList()
    private val netDisposable = CompositeJob()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    override fun onDestroyed() {
        netDisposable.cancel()
        super.onDestroyed()
    }

    private fun request(offset: Int) {
        netLoadingNow = true
        resolveRefreshingView()
        netDisposable.add(ownerInteractor.getMarketAlbums(
            accountId,
            owner_id,
            (offset - 1).coerceAtLeast(0),
            COUNT_PER_REQUEST
        )
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

    private fun onNetDataReceived(offset: Int, markets: List<MarketAlbum>) {
        cacheLoadingNow = false
        mEndOfContent = markets.isEmpty()
        netLoadingNow = false
        if (offset == 0) {
            mMarkets.clear()
            mMarkets.add(MarketAlbum(0, owner_id).setTitle(allProductString))
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

    override fun onGuiCreated(viewHost: IProductAlbumsView) {
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

    fun fireAlbumOpen(market_album: MarketAlbum) {
        view?.onMarketAlbumOpen(
            accountId,
            market_album
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