package dev.ragnarok.fenrir.fragment.videos

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadDestination.Companion.forVideo
import dev.ragnarok.fenrir.upload.UploadIntent
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.util.AppPerms.hasReadStoragePermission
import dev.ragnarok.fenrir.util.FindAtWithContent
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.SafeCallCheckInt
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCheck
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.Locale
import java.util.concurrent.TimeUnit

class VideosListPresenter(
    accountId: Long, ownerId: Long, albumId: Int, private val action: String?,
    albumTitle: String?, context: Context, savedInstanceState: Bundle?
) : AccountDependencyPresenter<IVideosListView>(accountId, savedInstanceState) {
    val ownerId: Long
    val albumId: Int
    private val data: MutableList<Video>
    private val interactor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    private val uploadManager: IUploadManager = Includes.uploadManager
    private val albumTitle: String?
    private val destination: UploadDestination = forVideo(
        if (IVideosListView.ACTION_SELECT.equals(action, ignoreCase = true)) 0 else 1,
        ownerId
    )
    private val uploadsData: MutableList<Upload>
    private val netDisposable = CompositeDisposable()
    private val cacheDisposable = CompositeDisposable()
    private val searcher: FindVideo
    private var sleepDataDisposable = Disposable.disposed()
    private var endOfContent = false
    private var intNextFrom: IntNextFrom
    private var hasActualNetData = false
    private var requestNow = false
    private var cacheNowLoading = false
    private fun sleep_search(q: String?) {
        if (requestNow || cacheNowLoading) return
        sleepDataDisposable.dispose()
        if (q.isNullOrEmpty()) {
            searcher.cancel()
        } else {
            if (!searcher.isSearchMode) {
                searcher.insertCache(data, intNextFrom.offset)
            }
            sleepDataDisposable = Single.just(Any())
                .delay(WEB_SEARCH_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .fromIOToMain()
                .subscribe({ searcher.do_search(q) }) { throwable ->
                    onListGetError(
                        throwable
                    )
                }
        }
    }

    fun fireSearchRequestChanged(q: String?) {
        sleep_search(q?.trim { it <= ' ' })
    }

    private fun onUploadsDataReceived(data: List<Upload>) {
        uploadsData.clear()
        uploadsData.addAll(data)
        view?.notifyDataSetChanged()
        resolveUploadDataVisibility()
    }

    private fun onUploadResults(pair: Pair<Upload, UploadResult<*>>) {
        val obj = pair.second.result as Video
        if (obj.id == 0) view?.customToast?.showToastError(
            R.string.error
        )
        else {
            view?.customToast?.showToast(R.string.uploaded)
            if (IVideosListView.ACTION_SELECT.equals(action, ignoreCase = true)) {
                view?.onUploaded(obj)
            } else fireRefresh()
        }
    }

    private fun onProgressUpdates(updates: List<IProgressUpdate>) {
        for (update in updates) {
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

    private fun onUploadStatusUpdate(upload: Upload) {
        val index = findIndexById(uploadsData, upload.getObjectId())
        if (index != -1) {
            view?.notifyUploadItemChanged(
                index
            )
        }
    }

    private fun onUploadsAdded(added: List<Upload>) {
        for (u in added) {
            if (destination.compareTo(u.destination)) {
                val index = uploadsData.size
                uploadsData.add(u)
                view?.notifyUploadItemsAdded(
                    index,
                    1
                )
            }
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

    private fun resolveRefreshingView() {
        resumedView?.displayLoading(
            requestNow
        )
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    internal fun setRequestNow(requestNow: Boolean) {
        this.requestNow = requestNow
        resolveRefreshingView()
    }

    fun doUpload() {
        if (hasReadStoragePermission(applicationContext)) {
            view?.startSelectUploadFileActivity(
                accountId
            )
        } else {
            view?.requestReadExternalStoragePermission()
        }
    }

    fun fireRemoveClick(upload: Upload) {
        uploadManager.cancel(upload.getObjectId())
    }

    fun fireReadPermissionResolved() {
        if (hasReadStoragePermission(applicationContext)) {
            view?.startSelectUploadFileActivity(
                accountId
            )
        }
    }

    private fun request(more: Boolean) {
        if (requestNow) return
        setRequestNow(true)
        val startFrom = if (more) intNextFrom else IntNextFrom(0)
        netDisposable.add(interactor[accountId, ownerId, albumId, COUNT, startFrom.offset]
            .fromIOToMain()
            .subscribe({
                val nextFrom = IntNextFrom(startFrom.offset + COUNT)
                onRequestResposnse(it, startFrom, nextFrom)
            }) { throwable -> onListGetError(throwable) })
    }

    internal fun onListGetError(throwable: Throwable) {
        setRequestNow(false)
        showError(throwable)
    }

    private fun onRequestResposnse(
        videos: List<Video>,
        startFrom: IntNextFrom,
        nextFrom: IntNextFrom
    ) {
        cacheDisposable.clear()
        cacheNowLoading = false
        hasActualNetData = true
        intNextFrom = nextFrom
        endOfContent = videos.isEmpty()
        if (startFrom.offset == 0) {
            data.clear()
            data.addAll(videos)
            view?.notifyDataSetChanged()
        } else {
            if (videos.nonNullNoEmpty()) {
                val startSize = data.size
                data.addAll(videos)
                view?.notifyDataAdded(
                    startSize,
                    videos.size
                )
            }
        }
        setRequestNow(false)
    }

    override fun onGuiCreated(viewHost: IVideosListView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data)
        viewHost.displayUploads(uploadsData)
        viewHost.setToolbarSubtitle(albumTitle)
        resolveUploadDataVisibility()
    }

    fun fireFileForUploadSelected(file: String?) {
        val intent = UploadIntent(accountId, destination)
            .setAutoCommit(true)
            .setFileUri(Uri.parse(file))
        uploadManager.enqueue(listOf(intent))
    }

    private fun loadAllFromCache() {
        cacheNowLoading = true
        cacheDisposable.add(interactor.getCachedVideos(accountId, ownerId, albumId)
            .fromIOToMain()
            .subscribe({ onCachedDataReceived(it) }) { obj -> obj.printStackTrace() })
    }

    private fun onCachedDataReceived(videos: List<Video>) {
        data.clear()
        data.addAll(videos)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.dispose()
        netDisposable.dispose()
        sleepDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireRefresh() {
        if (requestNow || cacheNowLoading) {
            return
        }
        if (searcher.isSearchMode) {
            searcher.reset()
        } else {
            request(false)
        }
    }

    fun fireOnVideoLongClick(position: Int, video: Video) {
        view?.doVideoLongClick(
            accountId,
            ownerId,
            ownerId == accountId,
            position,
            video
        )
    }

    fun fireScrollToEnd() {
        if (data.nonNullNoEmpty() && hasActualNetData && !cacheNowLoading && !requestNow) {
            if (searcher.isSearchMode) {
                searcher.do_search()
            } else if (!endOfContent) {
                request(true)
            }
        }
    }

    fun fireVideoClick(video: Video) {
        if (IVideosListView.ACTION_SELECT.equals(action, ignoreCase = true)) {
            view?.returnSelectionToParent(
                video
            )
        } else {
            view?.showVideoPreview(
                accountId,
                video
            )
        }
    }

    private fun fireEditVideo(context: Context, position: Int, video: Video) {
        val root = View.inflate(context, R.layout.entry_video_info, null)
        (root.findViewById<View>(R.id.edit_title) as TextInputEditText).setText(video.title)
        (root.findViewById<View>(R.id.edit_description) as TextInputEditText).setText(video.description)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _: DialogInterface?, _: Int ->
                val title =
                    (root.findViewById<View>(R.id.edit_title) as TextInputEditText).text.toString()
                val description =
                    (root.findViewById<View>(R.id.edit_description) as TextInputEditText).text.toString()
                appendDisposable(interactor.edit(
                    accountId, video.ownerId, video.id,
                    title, description
                ).fromIOToMain()
                    .subscribe({
                        data[position].setTitle(title).setDescription(description)
                        view?.notifyItemChanged(
                            position
                        )
                    }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun onAddComplete() {
        view?.showSuccessToast()
    }

    fun fireVideoOption(id: Int, video: Video, position: Int, context: Context) {
        when (id) {
            R.id.action_add_to_my_videos -> {
                netDisposable.add(interactor.addToMy(accountId, accountId, video.ownerId, video.id)
                    .fromIOToMain()
                    .subscribe({ onAddComplete() }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }

            R.id.action_edit -> {
                fireEditVideo(context, position, video)
            }

            R.id.action_delete_from_my_videos -> {
                netDisposable.add(interactor.delete(accountId, video.id, video.ownerId, accountId)
                    .fromIOToMain()
                    .subscribe({
                        data.removeAt(position)
                        view?.notifyItemRemoved(
                            position
                        )
                    }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }

            R.id.share_button -> {
                view?.displayShareDialog(
                    accountId,
                    video,
                    accountId != ownerId
                )
            }
        }
    }

    private inner class FindVideo(disposable: CompositeDisposable) : FindAtWithContent<Video>(
        disposable, SEARCH_VIEW_COUNT, SEARCH_COUNT
    ) {
        override fun search(offset: Int, count: Int): Single<List<Video>> {
            return interactor[accountId, ownerId, albumId, count, offset]
        }

        override fun onError(e: Throwable) {
            onListGetError(e)
        }

        override fun onResult(data: MutableList<Video>) {
            hasActualNetData = true
            val startSize = this@VideosListPresenter.data.size
            this@VideosListPresenter.data.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }

        override fun updateLoading(loading: Boolean) {
            setRequestNow(loading)
        }

        override fun clean() {
            data.clear()
            view?.notifyDataSetChanged()
        }

        override fun compare(data: Video, q: String): Boolean {
            return (safeCheck(
                data.title,
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.title?.lowercase(Locale.getDefault())?.contains(
                            q.lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                })
                    || safeCheck(
                data.description,
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.description?.lowercase(Locale.getDefault())?.contains(
                            q.lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                }))
        }

        override fun onReset(data: MutableList<Video>, offset: Int, isEnd: Boolean) {
            if (data.isEmpty()) {
                fireRefresh()
            } else {
                this@VideosListPresenter.data.clear()
                this@VideosListPresenter.data.addAll(data)
                intNextFrom.offset = offset
                endOfContent = isEnd
                view?.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val COUNT = 50
        private const val SEARCH_VIEW_COUNT = 15
        private const val SEARCH_COUNT = 100
        private const val WEB_SEARCH_DELAY = 1000
    }

    init {
        uploadsData = ArrayList(0)
        searcher = FindVideo(netDisposable)
        this.ownerId = ownerId
        this.albumId = albumId
        this.albumTitle = albumTitle
        intNextFrom = IntNextFrom(0)
        data = ArrayList()
        appendDisposable(uploadManager[accountId, destination]
            .fromIOToMain()
            .subscribe { data -> onUploadsDataReceived(data) })
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadsAdded(it) })
        appendDisposable(uploadManager.observeDeleting(true)
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadDeleted(it) })
        appendDisposable(uploadManager.observeResults()
            .filter {
                destination.compareTo(
                    it.first.destination
                )
            }
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadResults(it) })
        appendDisposable(uploadManager.observeStatus()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onUploadStatusUpdate(it) })
        appendDisposable(uploadManager.observeProgress()
            .observeOn(provideMainThreadScheduler())
            .subscribe { onProgressUpdates(it) })
        loadAllFromCache()
        request(false)
        if (IVideosListView.ACTION_SELECT.equals(action, ignoreCase = true)) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.do_upload_video)
                .setPositiveButton(R.string.button_yes) { _: DialogInterface?, _: Int -> doUpload() }
                .setNegativeButton(R.string.button_no, null)
                .show()
        }
    }
}