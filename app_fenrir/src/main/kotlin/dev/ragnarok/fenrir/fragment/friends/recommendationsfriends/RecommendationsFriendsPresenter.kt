package dev.ragnarok.fenrir.fragment.friends.recommendationsfriends

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.absownerslist.SimpleOwnersPresenter
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class RecommendationsFriendsPresenter(accountId: Long, savedInstanceState: Bundle?) :
    SimpleOwnersPresenter<ISimpleOwnersView>(accountId, savedInstanceState) {
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val actualDataDisposable = CompositeJob()
    private var actualDataLoading = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            actualDataLoading
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
        requestActualData()
    }

    private fun requestActualData() {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(relationshipInteractor.getRecommendations(accountId, 50)
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

    private fun onDataReceived(users: List<User>) {
        actualDataLoading = false
        data.clear()
        data.addAll(users)
        view?.notifyDataSetChanged()
        resolveRefreshingView()
    }

    override fun onUserScrolledToEnd() {}
    override fun onUserRefreshed() {
        actualDataDisposable.clear()
        requestActualData()
    }

    override fun onDestroyed() {
        actualDataDisposable.cancel()
        super.onDestroyed()
    }

}