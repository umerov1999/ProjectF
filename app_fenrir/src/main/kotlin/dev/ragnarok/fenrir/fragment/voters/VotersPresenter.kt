package dev.ragnarok.fenrir.fragment.voters

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IPollInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.fragment.absownerslist.SimpleOwnersPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.User
import io.reactivex.rxjava3.disposables.CompositeDisposable

class VotersPresenter(
    accountId: Long,
    private val ownerId: Long,
    private val pollId: Int,
    private val answerId: Long,
    private val isBoard: Boolean,
    savedInstanceState: Bundle?
) :
    SimpleOwnersPresenter<ISimpleOwnersView>(accountId, savedInstanceState) {
    private val pollsInteractor: IPollInteractor =
        InteractorFactory.createPollInteractor()
    private val actualDataDisposable = CompositeDisposable()
    private var endOfContent = false
    private var actualDataLoading = false
    private var doLoadTabs = false
    private var offset = 0
    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            actualDataLoading
        )
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        view?.updateTitle(R.string.voters)
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        offset = 0
        requestActualData()
    }

    private fun requestActualData() {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(pollsInteractor.getVoters(
            accountId,
            ownerId,
            pollId,
            if (isBoard) 1 else 0,
            listOf(answerId),
            offset,
            200
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

    private fun onDataReceived(users: List<User>) {
        actualDataLoading = false
        endOfContent = users.isEmpty()
        if (offset == 0) {
            data.clear()
            data.addAll(users)
            view?.notifyDataSetChanged()
        } else {
            val sizeBefore = data.size
            data.addAll(users)
            view?.notifyDataAdded(
                sizeBefore,
                users.size
            )
        }
        resolveRefreshingView()
        offset += 200
    }

    override fun onUserScrolledToEnd() {
        if (!endOfContent && !actualDataLoading && offset > 0) {
            requestActualData()
        }
    }

    override fun onUserRefreshed() {
        actualDataDisposable.clear()
        offset = 0
        requestActualData()
    }

    override fun onDestroyed() {
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

}