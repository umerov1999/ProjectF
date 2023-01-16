package dev.ragnarok.fenrir.fragment.userbanned

import android.os.Bundle
import dev.ragnarok.fenrir.Includes.blacklistRepository
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.domain.IAccountsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.BannedPart
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime

class UserBannedPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IUserBannedView>(accountId, savedInstanceState) {
    private val interactor: IAccountsInteractor = InteractorFactory.createAccountInteractor()
    private val owners: MutableList<Owner>
    private var endOfContent = false
    private var loadinNow = false
    private fun onOwnerRemoved(id: Long) {
        val index = findIndexById(owners, id)
        if (index != -1) {
            owners.removeAt(index)
            view?.notifyItemRemoved(
                index
            )
        }
    }

    private fun onOwnerAdded(owner: Owner) {
        owners.add(0, owner)
        view?.let {
            it.notifyItemsAdded(0, 1)
            it.scrollToPosition(0)
        }
    }

    override fun onGuiCreated(viewHost: IUserBannedView) {
        super.onGuiCreated(viewHost)
        viewHost.displayOwnerList(owners)
    }

    private fun onBannedPartReceived(offset: Int, part: BannedPart) {
        setLoadinNow(false)
        endOfContent = part.owners.isEmpty()
        if (offset == 0) {
            owners.clear()
            owners.addAll(part.owners)
            view?.notifyDataSetChanged()
        } else {
            val startSize = owners.size
            owners.addAll(part.owners)
            view?.notifyItemsAdded(
                startSize,
                part.owners.size
            )
        }
        endOfContent = endOfContent || part.getTotalCount() == owners.size
    }

    private fun onBannedPartGetError(throwable: Throwable) {
        setLoadinNow(false)
        showError(throwable)
    }

    private fun setLoadinNow(loadinNow: Boolean) {
        this.loadinNow = loadinNow
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            loadinNow
        )
    }

    private fun loadNextPart(offset: Int) {
        if (loadinNow) return
        setLoadinNow(true)
        appendDisposable(interactor.getBanned(accountId, 50, offset)
            .fromIOToMain()
            .subscribe(
                { part: BannedPart -> onBannedPartReceived(offset, part) }
            ) { throwable -> onBannedPartGetError(getCauseIfRuntime(throwable)) })
    }

    fun fireRefresh() {
        loadNextPart(0)
    }

    fun fireButtonAddClick() {
        view?.startUserSelection(accountId)
    }

    private fun onAddingComplete() {
        view?.showSuccessToast()
    }

    private fun onAddError(throwable: Throwable) {
        showError(throwable)
    }

    fun fireOwnersSelected(owners: ArrayList<Owner>) {
        if (owners.nonNullNoEmpty()) {
            appendDisposable(interactor.banOwners(accountId, owners)
                .fromIOToMain()
                .subscribe({ onAddingComplete() }) { throwable ->
                    onAddError(
                        getCauseIfRuntime(throwable)
                    )
                })
        }
    }

    fun fireScrollToEnd() {
        if (!loadinNow && !endOfContent) {
            loadNextPart(owners.size)
        }
    }

    private fun onRemoveComplete() {
        view?.showSuccessToast()
    }

    private fun onRemoveError(throwable: Throwable) {
        showError(throwable)
    }

    fun fireRemoveClick(owner: Owner) {
        appendDisposable(interactor.unbanOwner(accountId, owner.ownerId)
            .fromIOToMain()
            .subscribe({ onRemoveComplete() }) { throwable ->
                onRemoveError(
                    getCauseIfRuntime(throwable)
                )
            })
    }

    fun fireOwnerClick(owner: Owner) {
        view?.showOwnerProfile(
            accountId,
            owner
        )
    }

    init {
        owners = ArrayList()
        loadNextPart(0)
        val repository = blacklistRepository
        appendDisposable(repository.observeAdding()
            .filter { it.first == accountId }
            .map(Pair<Long, Owner>::second)
            .observeOn(provideMainThreadScheduler())
            .subscribe { onOwnerAdded(it) })
        appendDisposable(repository.observeRemoving()
            .filter { it.first == accountId }
            .map(Pair<Long, Long>::second)
            .observeOn(provideMainThreadScheduler())
            .subscribe { onOwnerRemoved(it) })
    }
}