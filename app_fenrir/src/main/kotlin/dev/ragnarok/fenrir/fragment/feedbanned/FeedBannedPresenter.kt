package dev.ragnarok.fenrir.fragment.feedbanned

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IFeedInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.absownerslist.SimpleOwnersPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.disposables.CompositeDisposable

class FeedBannedPresenter(
    accountId: Long,
    savedInstanceState: Bundle?
) :
    SimpleOwnersPresenter<ISimpleOwnersView>(accountId, savedInstanceState) {
    private val feedInteractor: IFeedInteractor =
        InteractorFactory.createFeedInteractor()
    private val actualDataDisposable = CompositeDisposable()
    private var endOfContent = false
    private var actualDataLoading = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            actualDataLoading
        )
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        view?.updateTitle(R.string.feed_ban)
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        requestActualData()
    }

    private fun requestActualData() {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(feedInteractor.getBanned(
            accountId
        )
            .fromIOToMain()
            .subscribe({ users -> onDataReceived(users) }) { t ->
                onDataGetError(
                    t
                )
            })
    }

    private fun onDataGetError(t: Throwable) {
        actualDataLoading = false
        resolveRefreshingView()
        showError(t)
    }

    fun fireRemove(owner: Owner) {
        actualDataDisposable.add(
            feedInteractor.deleteBan(accountId, listOf(owner.ownerId))
                .fromIOToMain()
                .subscribe({
                    val pos = Utils.indexOfOwner(data, owner)
                    data.removeAt(pos)
                    view?.notifyDataRemoved(pos, 1)
                }, {
                    showError(it)
                })
        )
    }

    private fun onDataReceived(users: List<Owner>) {
        actualDataLoading = false
        endOfContent = true
        data.clear()
        data.addAll(users)
        view?.notifyDataSetChanged()
        resolveRefreshingView()
    }

    override fun onUserScrolledToEnd() {
        if (!endOfContent && !actualDataLoading) {
            requestActualData()
        }
    }

    override fun onUserRefreshed() {
        actualDataDisposable.clear()
        requestActualData()
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

}
