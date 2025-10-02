package dev.ragnarok.fenrir.fragment.feed.ownerlist.owners

import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
import dev.ragnarok.fenrir.domain.IFeedInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.FeedOwners
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class FeedOwnersPresenter(accountId: Long, val listDbId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFeedOwnersView>(accountId, savedInstanceState) {
    private val data = FeedOwners(-1)
    private val tempDataStorage: ITempDataStorage = Includes.stores.tempStore()
    private val feedInteractor: IFeedInteractor = InteractorFactory.createFeedInteractor()
    private var loadingNow = false
    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveLoadingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        resumedView?.showLoading(
            loadingNow
        )
    }

    private fun loadAtLast(isRefreshing: Boolean) {
        setLoadingNow(true)
        load(isRefreshing)
    }

    private fun load(isRefreshing: Boolean) {
        appendJob(
            feedInteractor.getFeedListById(accountId, listDbId, isRefreshing)
                .fromIOToMain({
                    onDataReceived(it)
                }) { throwable -> onRequestError(throwable) })
    }

    private fun onRequestError(throwable: Throwable) {
        showError(getCauseIfRuntime(throwable))
        setLoadingNow(false)
    }

    private fun onDataReceived(list: FeedOwners?) {
        setLoadingNow(false)
        if (list == null) {
            return
        }
        data.setId(list.id).setOwners(list.owners).setTitle(list.title)
        view?.notifyDataSetChanged()
    }

    override fun onGuiCreated(viewHost: IFeedOwnersView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data.owners)
    }

    fun fireRefresh() {
        if (loadingNow) {
            return
        }
        loadAtLast(true)
    }


    fun fireAddToFaveOwners(owners: ArrayList<Owner>?) {
        if (data.id < 0 || owners.isNullOrEmpty()) {
            return
        }
        var isChanged = false
        for (i in owners) {
            val index = Utils.findIndexById(data.owners, i.ownerId)
            if (index < 0) {
                data.owners.add(i)
                isChanged = true
            }
        }
        if (isChanged) {
            setLoadingNow(true)
            val owners = LongArray(data.owners.size)
            for (i in data.owners.indices) {
                owners[i] = data.owners[i].ownerId
            }
            appendJob(
                tempDataStorage.updateFeedOwners(data.id, owners)
                    .fromIOToMain({
                        view?.notifyDataSetChanged()
                        setLoadingNow(false)
                        view?.customToast?.showToastSuccessBottom(R.string.success)
                    }, {
                        setLoadingNow(false)
                        loadAtLast(false)
                        showError(it)
                    })
            )
        }
    }

    fun fireFeedOwnersDelete(id: Long) {
        if (data.id < 0) {
            return
        }
        val index = Utils.findIndexById(data.owners, id)
        if (index >= 0) {
            setLoadingNow(true)
            data.owners.removeAt(index)
            val owners = LongArray(data.owners.size)
            for (i in data.owners.indices) {
                owners[i] = data.owners[i].ownerId
            }
            appendJob(
                tempDataStorage.updateFeedOwners(data.id, owners)
                    .fromIOToMain({
                        view?.notifyDataRemoved(index, 1)
                        setLoadingNow(false)
                        view?.customToast?.showToastSuccessBottom(R.string.success)
                    }, {
                        setLoadingNow(false)
                        loadAtLast(false)
                        showError(it)
                    })
            )
        }
    }

    fun fireFeedOwnerClick(owner: Owner) {
        view?.onOpenWall(accountId, owner.ownerId)
    }

    init {
        loadAtLast(false)
    }
}
