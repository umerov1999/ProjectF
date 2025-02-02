package dev.ragnarok.filegallery.fragment.tagowner

import dev.ragnarok.filegallery.Includes
import dev.ragnarok.filegallery.db.interfaces.ISearchRequestHelperStorage
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.tags.TagOwner
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain

class TagOwnerPresenter :
    RxSupportPresenter<ITagOwnerView>() {
    private val tagOwnerData: ArrayList<TagOwner> = ArrayList()
    private val storage: ISearchRequestHelperStorage =
        Includes.stores.searchQueriesStore()

    override fun onGuiCreated(viewHost: ITagOwnerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(tagOwnerData)
    }

    private fun loadActualData() {
        appendJob(
            storage.getTagOwners()
                .fromIOToMain(
                    { onActualDataReceived(it) },
                    { t -> onActualDataGetError(t) })
        )
    }

    fun addDir(owner: TagOwner, item: FileItem) {
        appendJob(
            storage.insertTagDir(owner.id, item)
                .fromIOToMain(
                    {
                        view?.successAdd(owner, item)
                    }, { t -> onActualDataGetError(t) })
        )
    }

    fun deleteTagOwner(pos: Int, owner: TagOwner) {
        appendJob(
            storage.deleteTagOwner(owner.id)
                .fromIOToMain(
                    {
                        tagOwnerData.removeAt(pos)
                        view?.notifyRemove(pos)
                    }, { t -> onActualDataGetError(t) })
        )
    }

    fun renameTagOwner(name: String?, owner: TagOwner) {
        if (name.isNullOrEmpty()) {
            return
        }
        appendJob(
            storage.updateNameTagOwner(owner.id, name)
                .fromIOToMain(
                    {
                        loadActualData()
                    }, { t -> onActualDataGetError(t) })
        )
    }

    private fun onActualDataGetError(t: Throwable) {
        view?.customToast?.showToastThrowable(t)
    }

    private fun onActualDataReceived(data: List<TagOwner>) {
        tagOwnerData.clear()
        tagOwnerData.addAll(data)
        view?.notifyChanges()
    }

    fun addOwner(name: String?) {
        if (name.isNullOrEmpty()) {
            return
        }
        appendJob(
            storage.insertTagOwner(name)
                .fromIOToMain(
                    {
                        tagOwnerData.add(0, it)
                        view?.notifyAdd(0)
                    }, { t -> onActualDataGetError(t) })
        )
    }

    init {
        loadActualData()
    }
}
