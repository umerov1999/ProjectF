package dev.ragnarok.fenrir.fragment.docs

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.SendAttachmentsActivity.Companion.startForSendAttachments
import dev.ragnarok.fenrir.domain.IDocsInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.model.DocFilter
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.LocalPhoto
import dev.ragnarok.fenrir.model.menu.options.DocsOption
import dev.ragnarok.fenrir.place.PlaceFactory.getOwnerWallPlace
import dev.ragnarok.fenrir.place.PlaceUtil.goToPostCreation
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadDestination.Companion.forDocuments
import dev.ragnarok.fenrir.upload.UploadIntent
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.upload.UploadUtils.createIntents
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.intValueIn
import dev.ragnarok.fenrir.util.Utils.shareLink
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.toast.CustomToast.Companion.createCustomToast
import kotlinx.coroutines.flow.filter

class DocsListPresenter(
    accountId: Long,
    ownerId: Long,
    action: String?,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IDocListView>(accountId, savedInstanceState) {
    private val mOwnerId: Long = ownerId
    private val mLoader = CompositeJob()
    private val mDocuments: MutableList<Document> = ArrayList()
    private val mAction: String? = action
    private val filters: MutableList<DocFilter>
    private val docsInteractor: IDocsInteractor = InteractorFactory.createDocsInteractor()
    private val uploadManager: IUploadManager = Includes.uploadManager
    private val destination: UploadDestination = forDocuments(ownerId)
    private val uploadsData: MutableList<Upload> = ArrayList(0)
    private val requestHolder = CompositeJob()
    private var requestNow = false
    private var cacheLoadingNow = false
    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putInt(SAVE_FILTER, selectedFilter)
    }

    private fun createFilters(selectedType: Int): MutableList<DocFilter> {
        val data: MutableList<DocFilter> = ArrayList()
        data.add(DocFilter(DocFilter.Type.ALL, R.string.doc_filter_all))
        data.add(DocFilter(DocFilter.Type.TEXT, R.string.doc_filter_text))
        data.add(DocFilter(DocFilter.Type.ARCHIVE, R.string.doc_filter_archive))
        data.add(DocFilter(DocFilter.Type.GIF, R.string.doc_filter_gif))
        data.add(DocFilter(DocFilter.Type.IMAGE, R.string.doc_filter_image))
        data.add(DocFilter(DocFilter.Type.AUDIO, R.string.doc_filter_audio))
        data.add(DocFilter(DocFilter.Type.VIDEO, R.string.doc_filter_video))
        data.add(DocFilter(DocFilter.Type.BOOKS, R.string.doc_filter_books))
        data.add(DocFilter(DocFilter.Type.OTHER, R.string.doc_filter_other))
        for (filter in data) {
            filter.setActive(selectedType == filter.type)
        }
        return data
    }

    private fun onUploadsDataReceived(data: List<Upload>) {
        uploadsData.clear()
        uploadsData.addAll(data)
        view?.notifyDataSetChanged()
        resolveUploadDataVisibility()
    }

    private fun onUploadResults(pair: Pair<Upload, UploadResult<*>>) {
        mDocuments.add(0, pair.second.result as Document)
        view?.notifyDataSetChanged()
    }

    private fun onProgressUpdates(updates: IProgressUpdate?) {
        updates?.let { update ->
            val index = findIndexById(uploadsData, update.id)
            if (index != -1) {
                view?.notifyUploadProgressChanged(
                    index,
                    update.progress,
                    true
                )
            }
        }
    }

    fun onMenuSelect(context: Context, index: Int, doc: Document, option: Option) {
        when (option.id) {
            DocsOption.open_item_doc -> fireDocClick(doc)
            DocsOption.share_item_doc -> share(context, doc)
            DocsOption.add_item_doc -> {
                val docsInteractor = InteractorFactory.createDocsInteractor()
                val accessKey = doc.accessKey
                appendJob(
                    docsInteractor.add(
                        accountId,
                        doc.id,
                        doc.ownerId,
                        accessKey
                    )
                        .fromIOToMain({
                            createCustomToast(context).setDuration(
                                Toast.LENGTH_LONG
                            ).showToastSuccessBottom(R.string.added)
                        }) { t ->
                            showError(getCauseIfRuntime(t))
                        })
            }

            DocsOption.delete_item_doc -> MaterialAlertDialogBuilder(context)
                .setTitle(R.string.remove_confirm)
                .setMessage(R.string.doc_remove_confirm_message)
                .setPositiveButton(R.string.button_yes) { _, _ ->
                    doRemove(
                        doc,
                        index
                    )
                }
                .setNegativeButton(R.string.cancel, null)
                .show()

            DocsOption.go_to_owner_doc -> getOwnerWallPlace(
                accountId,
                doc.ownerId,
                null
            ).tryOpenWith(context)
        }
    }

    fun fireMenuClick(index: Int, doc: Document) {
        view?.onMenuClick(index, doc, isMy)
    }

    private fun doRemove(doc: Document, index: Int) {
        appendJob(
            docsInteractor.delete(accountId, doc.id, doc.ownerId)
                .fromIOToMain({
                    mDocuments.removeAt(index)
                    view?.notifyDataRemoved(index)
                }) { })
    }

    internal fun share(context: Context, document: Document) {
        val items = arrayOf(
            getString(R.string.share_link),
            getString(R.string.repost_send_message),
            getString(R.string.repost_to_wall)
        )
        MaterialAlertDialogBuilder(context)
            .setItems(items) { _, i ->
                when (i) {
                    0 -> shareLink(
                        (context as Activity),
                        String.format("vk.com/doc%s_%s", document.ownerId, document.id),
                        document.title
                    )

                    1 -> startForSendAttachments(context, accountId, document)
                    2 -> postToMyWall(context, document)
                }
            }
            .setCancelable(true)
            .setTitle(R.string.share_document_title)
            .show()
    }

    private fun postToMyWall(context: Context, document: Document) {
        val models: List<AbsModel> = listOf(document)
        goToPostCreation((context as Activity), accountId, accountId, EditingPostType.TEMP, models)
    }

    private val isMy: Boolean
        get() = accountId == mOwnerId

    private fun onUploadStatusUpdate(upload: Upload) {
        val index = findIndexById(uploadsData, upload.getObjectId())
        if (index != -1) {
            view?.notifyUploadItemChanged(
                index
            )
        }
    }

    private fun onUploadsAdded(added: List<Upload>) {
        var count = 0
        val cur = uploadsData.size
        for (u in added) {
            if (destination.compareTo(u.destination)) {
                val index = findIndexById(uploadsData, u.getObjectId())
                if (index == -1) {
                    uploadsData.add(u)
                    count++
                }
            }
        }
        if (count > 0) {
            view?.notifyUploadItemsAdded(cur, count)
        }
        resolveUploadDataVisibility()
    }

    private fun onUploadDeleted(ids: IntArray) {
        for (id in ids) {
            val index = findIndexById(uploadsData, id)
            if (index != -1) {
                uploadsData.removeAt(index)
                view?.notifyUploadItemRemoved(
                    index
                )
            }
        }
        resolveUploadDataVisibility()
    }

    private fun resolveUploadDataVisibility() {
        view?.setUploadDataVisible(uploadsData.isNotEmpty())
    }

    private fun setCacheLoadingNow(cacheLoadingNow: Boolean) {
        this.cacheLoadingNow = cacheLoadingNow
        resolveRefreshingView()
    }

    private fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveRefreshingView()
    }

    private val selectedFilter: Int
        get() {
            for (filter in filters) {
                if (filter.isActive) {
                    return filter.type
                }
            }
            return DocFilter.Type.ALL
        }

    private fun requestAll() {
        setRequestNow(true)
        val filter = selectedFilter
        requestHolder.add(
            docsInteractor.request(accountId, mOwnerId, filter)
                .fromIOToMain({ data -> onNetDataReceived(data) }) { throwable ->
                    onRequestError(
                        getCauseIfRuntime(throwable)
                    )
                })
    }

    private fun onRequestError(throwable: Throwable) {
        setRequestNow(false)
        showError(throwable)
    }

    private fun onCacheDataReceived(data: List<Document>) {
        setCacheLoadingNow(false)
        mDocuments.clear()
        mDocuments.addAll(data)
        safelyNotifyDataSetChanged()
    }

    private fun onNetDataReceived(data: List<Document>) {
        // cancel db loading if active
        mLoader.clear()
        cacheLoadingNow = false
        requestNow = false
        resolveRefreshingView()
        mDocuments.clear()
        mDocuments.addAll(data)
        safelyNotifyDataSetChanged()
    }

    override fun onGuiCreated(viewHost: IDocListView) {
        super.onGuiCreated(viewHost)
        viewHost.displayUploads(uploadsData)
        viewHost.displayFilterData(filters)
        resolveUploadDataVisibility()
        resolveRefreshingView()
        resolveDocsListData()
    }

    private fun loadAll() {
        setCacheLoadingNow(true)
        val filter = selectedFilter
        mLoader.add(
            docsInteractor.getCacheData(accountId, mOwnerId, filter)
                .fromIOToMain({ data -> onCacheDataReceived(data) }) { throwable ->
                    onLoadError(
                        getCauseIfRuntime(throwable)
                    )
                })
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(isNowLoading)
    }

    private val isNowLoading: Boolean
        get() = cacheLoadingNow || requestNow

    private fun safelyNotifyDataSetChanged() {
        resolveDocsListData()
    }

    private fun resolveDocsListData() {
        view?.displayData(
            mDocuments,
            isImagesOnly
        )
    }

    private val isImagesOnly: Boolean
        get() = intValueIn(selectedFilter, DocFilter.Type.IMAGE, DocFilter.Type.GIF)

    private fun onLoadError(throwable: Throwable) {
        throwable.printStackTrace()
        setCacheLoadingNow(false)
        showError(throwable)
        resolveRefreshingView()
    }

    override fun onDestroyed() {
        mLoader.cancel()
        requestHolder.cancel()
        super.onDestroyed()
    }

    fun fireRefresh() {
        mLoader.clear()
        cacheLoadingNow = false
        requestAll()
    }

    fun fireButtonAddClick() {
        if (hasReadStoragePermission(applicationContext)) {
            view?.startSelectUploadFileActivity(
                accountId
            )
        } else {
            view?.requestReadExternalStoragePermission()
        }
    }

    fun fireDocClick(doc: Document) {
        if (ACTION_SELECT == mAction) {
            val selected = ArrayList<Document>(1)
            selected.add(doc)
            view?.returnSelection(selected)
        } else {
            if (doc.isGif && doc.hasValidGifVideoLink()) {
                val gifs = ArrayList<Document>()
                var selectedIndex = 0
                for (i in mDocuments.indices) {
                    val d = mDocuments[i]
                    if (d.isGif && d.hasValidGifVideoLink()) {
                        gifs.add(d)
                        if (d.id == doc.id && d.ownerId == doc.ownerId) {
                            selectedIndex = gifs.size - 1
                        }
                    }
                }
                if (selectedIndex <= 0) {
                    selectedIndex = 0
                }
                if (gifs.isEmpty()) {
                    selectedIndex = 0
                    gifs.add(doc)
                }
                view?.goToGifPlayer(
                    accountId,
                    gifs,
                    selectedIndex
                )
            } else {
                view?.openDocument(
                    accountId,
                    doc
                )
            }
        }
    }

    fun fireReadPermissionResolved() {
        if (hasReadStoragePermission(applicationContext)) {
            view?.startSelectUploadFileActivity(
                accountId
            )
        }
    }

    fun fireFileForUploadSelected(file: String?) {
        val intent = UploadIntent(accountId, destination)
            .setAutoCommit(true)
            .setFileUri(file?.toUri())
        uploadManager.enqueue(listOf(intent))
    }

    fun fireRemoveClick(upload: Upload) {
        uploadManager.cancel(upload.getObjectId())
    }

    fun fireFilterClick(entry: DocFilter) {
        for (filter in filters) {
            filter.setActive(entry.type == filter.type)
        }
        view?.notifyFiltersChanged()
        loadAll()
        requestAll()
    }

    fun pleaseNotifyViewAboutAdapterType() {
        view?.setAdapterType(isImagesOnly)
    }

    fun fireLocalPhotosForUploadSelected(photos: ArrayList<LocalPhoto>) {
        val intents: List<UploadIntent> =
            createIntents(accountId, destination, photos, Upload.IMAGE_SIZE_FULL, true)
        uploadManager.enqueue(intents)
    }

    companion object {
        const val ACTION_SELECT = "dev.ragnarok.fenrir.select.docs"
        const val ACTION_SHOW = "dev.ragnarok.fenrir.show.docs"
        private const val SAVE_FILTER = "save_filter"
    }

    init {
        appendJob(
            uploadManager[accountId, destination]
                .fromIOToMain { data -> onUploadsDataReceived(data) })
        appendJob(
            uploadManager.observeAdding()
                .sharedFlowToMain { onUploadsAdded(it) })
        appendJob(
            uploadManager.observeDeleting(true)
                .sharedFlowToMain { onUploadDeleted(it) })
        appendJob(
            uploadManager.observeResults()
                .filter {
                    destination.compareTo(
                        it.first.destination
                    )
                }
                .sharedFlowToMain { onUploadResults(it) })
        appendJob(
            uploadManager.observeStatus()
                .sharedFlowToMain { onUploadStatusUpdate(it) })
        appendJob(
            uploadManager.observeProgress()
                .sharedFlowToMain { onProgressUpdates(it) })
        val filter = savedInstanceState?.getInt(SAVE_FILTER) ?: DocFilter.Type.ALL
        filters = createFilters(filter)
        loadAll()
        requestAll()
    }
}