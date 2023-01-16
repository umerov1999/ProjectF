package dev.ragnarok.fenrir.fragment.friends.recommendationsfriends

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.absownerslist.SimpleOwnersPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.User
import io.reactivex.rxjava3.disposables.CompositeDisposable

class RecommendationsFriendsPresenter(accountId: Long, savedInstanceState: Bundle?) :
    SimpleOwnersPresenter<ISimpleOwnersView>(accountId, savedInstanceState) {
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val actualDataDisposable = CompositeDisposable()
    private var actualDataLoading = false
    private var doLoadTabs = false
    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            actualDataLoading
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
        requestActualData()
    }

    private fun requestActualData() {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(relationshipInteractor.getRecommendations(accountId, 50)
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
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

}