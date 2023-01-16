package dev.ragnarok.fenrir.fragment.productalbums

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.MarketAlbum
import io.reactivex.rxjava3.disposables.CompositeDisposable

class ProductAlbumsPresenter(
    accountId: Long,
    private val owner_id: Long,
    private val context: Context,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IProductAlbumsView>(accountId, savedInstanceState) {
    private val ownerInteractor: IOwnersRepository = owners
    private val mMarkets: ArrayList<MarketAlbum> = ArrayList()
    private val netDisposable = CompositeDisposable()
    private var mEndOfContent = false
    private var cacheLoadingNow = false
    private var netLoadingNow = false
    private fun resolveRefreshingView() {
        view?.showRefreshing(
            netLoadingNow
        )
    }

    override fun onDestroyed() {
        netDisposable.dispose()
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

    private fun onNetDataReceived(offset: Int, markets: List<MarketAlbum>) {
        cacheLoadingNow = false
        mEndOfContent = markets.isEmpty()
        netLoadingNow = false
        if (offset == 0) {
            mMarkets.clear()
            mMarkets.add(MarketAlbum(0, owner_id).setTitle(context.getString(R.string.markets_all)))
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