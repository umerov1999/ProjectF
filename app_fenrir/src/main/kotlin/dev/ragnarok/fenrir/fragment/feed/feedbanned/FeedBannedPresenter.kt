package dev.ragnarok.fenrir.fragment.feed.feedbanned

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IFeedInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.absownerslist.SimpleOwnersPresenter
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class FeedBannedPresenter(
    accountId: Long,
    savedInstanceState: Bundle?
) :
    SimpleOwnersPresenter<ISimpleOwnersView>(accountId, savedInstanceState) {
    private val feedInteractor: IFeedInteractor =
        InteractorFactory.createFeedInteractor()
    private val actualDataDisposable = CompositeJob()
    private var endOfContent = false
    private var actualDataLoading = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            actualDataLoading
        )
    }

    override fun onGuiResumed() {
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
            .fromIOToMain({ users -> onDataReceived(users) }) { t ->
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
                .fromIOToMain({
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
        actualDataDisposable.cancel()
        super.onDestroyed()
    }

}
