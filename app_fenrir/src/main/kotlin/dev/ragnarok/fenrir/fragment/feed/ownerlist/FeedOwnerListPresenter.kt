package dev.ragnarok.fenrir.fragment.feed.ownerlist

import android.content.Context
import android.os.Bundle
import android.text.InputType
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.interfaces.ITempDataStorage
import dev.ragnarok.fenrir.db.model.entity.FeedOwnersEntity
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.InputTextDialog
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class FeedOwnerListPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFeedOwnerListView>(accountId, savedInstanceState) {
    private val data: MutableList<FeedOwnersEntity> = ArrayList()
    private val tempDataStorage: ITempDataStorage = Includes.stores.tempStore()
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

    private fun loadAtLast() {
        setLoadingNow(true)
        load()
    }

    private fun load() {
        appendJob(
            tempDataStorage.getFeedOwners()
                .fromIOToMain({
                    onDataReceived(it)
                }) { throwable -> onRequestError(throwable) })
    }

    private fun onRequestError(throwable: Throwable) {
        showError(getCauseIfRuntime(throwable))
        setLoadingNow(false)
    }

    private fun onDataReceived(lists: List<FeedOwnersEntity>) {
        setLoadingNow(false)
        data.clear()
        data.addAll(lists)
        view?.notifyDataSetChanged()
    }

    override fun onGuiCreated(viewHost: IFeedOwnerListView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
    }

    fun fireRefresh() {
        if (loadingNow) {
            return
        }
        loadAtLast()
    }


    fun fireAddToFaveOwnerList(context: Context, owners: ArrayList<Owner>?) {
        if (owners.isNullOrEmpty()) {
            return
        }
        val iIds = LongArray(owners.size)
        for (i in iIds.indices) {
            iIds[i] = owners[i].ownerId
        }
        InputTextDialog.Builder(context)
            .setTitleRes(R.string.set_news_list_title)
            .setAllowEmpty(false)
            .setInputType(InputType.TYPE_CLASS_TEXT)
            .setCallback(object : InputTextDialog.Callback {
                override fun onChanged(newValue: String?) {
                    appendJob(
                        tempDataStorage.addFeedOwners(
                            newValue?.trim().orEmpty(),
                            iIds
                        )
                            .fromIOToMain({
                                data.add(0, it)
                                view?.customToast?.showToastSuccessBottom(R.string.success)
                                view?.notifyDataAdded(0, 1)
                            }) { i ->
                                showError(i)
                            })
                }

                override fun onCanceled() {

                }
            })
            .show()
    }

    fun fireFeedOwnerListDelete(index: Int, id: Long) {
        appendJob(
            tempDataStorage.deleteFeedOwners(id)
                .fromIOToMain({
                    data.removeAt(index)
                    view?.notifyDataRemoved(index, 1)
                    view?.customToast?.showToastSuccessBottom(R.string.success)
                }, {
                    showError(it)
                })
        )
    }

    fun fireFeedOwnerListRename(index: Int, id: Long, newTitle: String?) {
        appendJob(
            tempDataStorage.renameFeedOwners(id, newTitle)
                .fromIOToMain({
                    data[index].setTitle(newTitle)
                    view?.notifyDataChanged(index, 1)
                }, {
                    showError(it)
                })
        )
    }

    fun fireFeedOwnerListClick(listId: Long) {
        view?.feedOwnerListOpen(accountId, listId)
    }

    init {
        loadAtLast()
    }
}
