package dev.ragnarok.fenrir.fragment.products.marketview

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Market
import dev.ragnarok.fenrir.model.Peer
import dev.ragnarok.fenrir.push.OwnerInfo.Companion.getRx
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class MarketViewPresenter(
    accountId: Long,
    private var mMarket: Market,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IMarketViewView>(accountId, savedInstanceState) {
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val ownerInteractor: IOwnersRepository = owners
    private var loadingNow = false
    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveLoadingView()
    }

    private fun refreshPollData() {
        if (loadingNow) return
        setLoadingNow(true)
        val ids: Collection<AccessIdPair> =
            listOf(AccessIdPair(mMarket.id, mMarket.owner_id, mMarket.access_key))
        appendJob(
            ownerInteractor.getMarketById(accountId, ids)
                .fromIOToMain({ market -> onMarketInfoUpdated(market) }) { t ->
                    onLoadingError(
                        t
                    )
                })
    }

    private fun onLoadingError(t: Throwable) {
        showError(t)
        setLoadingNow(false)
    }

    private fun onMarketInfoUpdated(market: List<Market>) {
        if (market.isEmpty()) {
            return
        }
        mMarket = market[0]
        setLoadingNow(false)
        resolveMarketView()
    }

    private fun resolveLoadingView() {
        view?.displayLoading(loadingNow)
    }

    private fun resolveMarketView() {
        view?.displayMarket(
            mMarket,
            accountId
        )
    }

    override fun onGuiCreated(viewHost: IMarketViewView) {
        super.onGuiCreated(viewHost)
        resolveLoadingView()
        resolveMarketView()
    }

    fun fireSendMarket(market: Market) {
        view?.sendMarket(
            accountId,
            market
        )
    }

    fun fireWriteToMarketer(market: Market, context: Context) {
        appendJob(
            getRx(context, accountId, market.owner_id)
                .fromIOToMain({ userInfo ->
                    val peer = Peer(Peer.fromOwnerId(userInfo.owner.ownerId))
                        .setAvaUrl(userInfo.owner.maxSquareAvatar)
                        .setTitle(userInfo.owner.fullName)
                    view?.onWriteToMarketer(
                        accountId,
                        market,
                        peer
                    )
                }) {
                    val peer = Peer(Peer.fromOwnerId(market.owner_id))
                        .setAvaUrl(market.thumb_photo)
                        .setTitle(market.title)
                    view?.onWriteToMarketer(
                        accountId,
                        market,
                        peer
                    )
                })
    }

    private fun onFaveSuccess() {
        mMarket.setIs_favorite(!mMarket.isIs_favorite)
        resolveMarketView()
    }

    fun fireFaveClick() {
        if (!mMarket.isIs_favorite) {
            appendJob(
                faveInteractor.addProduct(
                    accountId,
                    mMarket.id,
                    mMarket.owner_id,
                    mMarket.access_key
                )
                    .fromIOToMain({ onFaveSuccess() }) { t -> onLoadingError(t) })
        } else {
            appendJob(
                faveInteractor.removeProduct(accountId, mMarket.id, mMarket.owner_id)
                    .fromIOToMain({ onFaveSuccess() }) { t ->
                        onLoadingError(
                            t
                        )
                    })
        }
    }

    init {
        refreshPollData()
    }
}