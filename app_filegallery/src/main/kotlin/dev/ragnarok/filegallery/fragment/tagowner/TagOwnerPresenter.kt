package dev.ragnarok.filegallery.fragment.tagowner

import android.os.Bundle
import dev.ragnarok.filegallery.Includes
import dev.ragnarok.filegallery.db.interfaces.ISearchRequestHelperStorage
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.fromIOToMain
import dev.ragnarok.filegallery.model.FileItem
import dev.ragnarok.filegallery.model.tags.TagOwner

class TagOwnerPresenter(savedInstanceState: Bundle?) :
    RxSupportPresenter<ITagOwnerView>(savedInstanceState) {
    private val tagOwnerData: ArrayList<TagOwner> = ArrayList()
    private val storage: ISearchRequestHelperStorage =
        Includes.stores.searchQueriesStore()

    override fun onGuiCreated(viewHost: ITagOwnerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(tagOwnerData)
    }

    private fun loadActualData() {
        appendDisposable(
            storage.getTagOwners()
                .fromIOToMain()
                .subscribe({ onActualDataReceived(it) },
                    { t: Throwable -> onActualDataGetError(t) })
        )
    }

    fun addDir(owner: TagOwner, item: FileItem) {
        appendDisposable(
            storage.insertTagDir(owner.id, item)
                .fromIOToMain()
                .subscribe(
                    {
                        view?.successAdd(owner, item)
                    }, { t: Throwable -> onActualDataGetError(t) })
        )
    }

    fun deleteTagOwner(pos: Int, owner: TagOwner) {
        appendDisposable(
            storage.deleteTagOwner(owner.id)
                .fromIOToMain()
                .subscribe(
                    {
                        tagOwnerData.removeAt(pos)
                        view?.notifyRemove(pos)
                    }, { t: Throwable -> onActualDataGetError(t) })
        )
    }

    fun renameTagOwner(name: String?, owner: TagOwner) {
        if (name.isNullOrEmpty()) {
            return
        }
        appendDisposable(
            storage.updateNameTagOwner(owner.id, name)
                .fromIOToMain()
                .subscribe(
                    {
                        loadActualData()
                    }, { t: Throwable -> onActualDataGetError(t) })
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
        appendDisposable(
            storage.insertTagOwner(name)
                .fromIOToMain()
                .subscribe(
                    {
                        tagOwnerData.add(0, it)
                        view?.notifyAdd(0)
                    }, { t: Throwable -> onActualDataGetError(t) })
        )
    }

    init {
        loadActualData()
    }
}
