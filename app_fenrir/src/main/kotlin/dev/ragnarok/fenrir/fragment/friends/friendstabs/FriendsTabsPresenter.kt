package dev.ragnarok.fenrir.fragment.friends.friendstabs

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.FriendsCounters
import dev.ragnarok.fenrir.model.Owner

class FriendsTabsPresenter(
    accountId: Long,
    private val userId: Long,
    counters: FriendsCounters?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IFriendsTabsView>(accountId, savedInstanceState) {
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val ownersRepository: IOwnersRepository = owners
    private var counters: FriendsCounters? = null
    private var owner: Owner? = null
    private fun requestOwnerInfo() {
        appendDisposable(ownersRepository.getBaseOwnerInfo(
            accountId,
            userId,
            IOwnersRepository.MODE_ANY
        )
            .fromIOToMain()
            .subscribe({ owner -> onOwnerInfoReceived(owner) }) { })
    }

    fun fireFriendsBirthday() {
        view?.onFriendsBirthday(accountId, userId)
    }

    fun isMe(): Boolean {
        return accountId == userId
    }

    private fun onOwnerInfoReceived(owner: Owner) {
        this.owner = owner
        view?.displayUserNameAtToolbar(
            owner.fullName
        )
    }

    private fun requestCounters() {
        appendDisposable(relationshipInteractor.getFriendsCounters(accountId, userId)
            .fromIOToMain()
            .subscribe({ counters -> onCountersReceived(counters) }) { t ->
                onCountersGetError(
                    t
                )
            })
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        view?.setDrawerFriendsSectionSelected(
            userId == accountId
        )
    }

    private fun onCountersGetError(t: Throwable) {
        view?.displayCounters(
            counters ?: FriendsCounters(0, 0, 0, 0)
        )
        showError(t)
    }

    private fun onCountersReceived(counters: FriendsCounters) {
        this.counters = counters
        view?.displayCounters(
            counters
        )
    }

    override fun onGuiCreated(viewHost: IFriendsTabsView) {
        super.onGuiCreated(viewHost)
        viewHost.configTabs(accountId, userId, userId != accountId)
        viewHost.displayCounters(counters ?: FriendsCounters(0, 0, 0, 0))
    }

    companion object {
        private const val SAVE_COUNTERS = "save_counters"
    }

    init {
        if (savedInstanceState != null) {
            this.counters = savedInstanceState.getParcelableCompat(SAVE_COUNTERS)
        } else {
            this.counters = counters
        }
        if (this.counters == null) {
            this.counters = FriendsCounters(0, 0, 0, 0)
            requestCounters()
        }
        if (owner == null && userId != accountId) {
            requestOwnerInfo()
        }
    }
}