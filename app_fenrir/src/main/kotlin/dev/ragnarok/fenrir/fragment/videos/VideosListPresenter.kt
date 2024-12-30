package dev.ragnarok.fenrir.fragment.videos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
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
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCheck
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import java.util.Locale

class VideosListPresenter(
    accountId: Long, val ownerId: Long, val albumId: Int, private val action: String?,
    private val albumTitle: String?, context: Context, savedInstanceState: Bundle?
) : AccountDependencyPresenter<IVideosListView>(accountId, savedInstanceState) {
    private val data: MutableList<Video>
    private val interactor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    private val uploadManager: IUploadManager = Includes.uploadManager
    private val destination: UploadDestination = forVideo(
        if (IVideosListView.ACTION_SELECT.equals(action, ignoreCase = true)) 0 else 1,
        ownerId
    )
    private val uploadsData: MutableList<Upload> = ArrayList(0)
    private val netDisposable = CompositeJob()
    private val cacheDisposable = CompositeJob()
    private val searcher: FindVideo = FindVideo(netDisposable)
    private var sleepDataDisposable = CancelableJob()
    private var endOfContent = false
    private var intNextFrom: IntNextFrom
    private var hasActualNetData = false
    private var requestNow = false
    private var cacheNowLoading = false
    private fun sleep_search(q: String?) {
        if (requestNow || cacheNowLoading) return
        sleepDataDisposable.cancel()
        if (q.isNullOrEmpty()) {
            searcher.cancel()
        } else {
            if (!searcher.isSearchMode) {
                searcher.insertCache(data, intNextFrom.offset)
            }
            sleepDataDisposable += delayTaskFlow(WEB_SEARCH_DELAY.toLong())
                .toMain { searcher.do_search(q) }
        }
    }

    fun fireSearchRequestChanged(q: String?) {
        sleep_search(q?.trim())
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

    private fun resolveRefreshingView() {
        resumedView?.displayLoading(
            requestNow
        )
    }

    override fun onGuiResumed() {
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
            .fromIOToMain({
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
            .fromIOToMain({ onCachedDataReceived(it) }) { obj -> obj.printStackTrace() })
    }

    private fun onCachedDataReceived(videos: List<Video>) {
        data.clear()
        data.addAll(videos)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        netDisposable.cancel()
        sleepDataDisposable.cancel()
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
        root.findViewById<TextInputEditText>(R.id.edit_title).setText(video.title)
        root.findViewById<TextInputEditText>(R.id.edit_description).setText(video.description)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                val title = root.findViewById<TextInputEditText>(R.id.edit_title).text.toString()
                val description =
                    root.findViewById<TextInputEditText>(R.id.edit_description).text.toString()
                appendJob(interactor.edit(
                    accountId, video.ownerId, video.id,
                    title, description
                ).fromIOToMain({
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
                    .fromIOToMain({ onAddComplete() }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }

            R.id.action_edit -> {
                fireEditVideo(context, position, video)
            }

            R.id.action_delete_from_my_videos -> {
                netDisposable.add(interactor.delete(accountId, video.id, video.ownerId, accountId)
                    .fromIOToMain({
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

    private inner class FindVideo(disposable: CompositeJob) : FindAtWithContent<Video>(
        disposable, SEARCH_VIEW_COUNT, SEARCH_COUNT
    ) {
        override fun search(offset: Int, count: Int): Flow<List<Video>> {
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
                data.title
            ) {
                data.title?.lowercase(Locale.getDefault())?.contains(
                    q.lowercase(
                        Locale.getDefault()
                    )
                ) == true
            }
                    || safeCheck(
                data.description
            ) {
                data.description?.lowercase(Locale.getDefault())?.contains(
                    q.lowercase(
                        Locale.getDefault()
                    )
                ) == true
            })
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
        intNextFrom = IntNextFrom(0)
        data = ArrayList()
        appendJob(uploadManager[accountId, destination]
            .fromIOToMain { data -> onUploadsDataReceived(data) })
        appendJob(uploadManager.observeAdding()
            .sharedFlowToMain { onUploadsAdded(it) })
        appendJob(uploadManager.observeDeleting(true)
            .sharedFlowToMain { onUploadDeleted(it) })
        appendJob(uploadManager.observeResults()
            .filter {
                destination.compareTo(
                    it.first.destination
                )
            }
            .sharedFlowToMain { onUploadResults(it) })
        appendJob(uploadManager.observeStatus()
            .sharedFlowToMain { onUploadStatusUpdate(it) })
        appendJob(uploadManager.observeProgress()
            .sharedFlowToMain { onProgressUpdates(it) })
        loadAllFromCache()
        request(false)
        if (IVideosListView.ACTION_SELECT.equals(action, ignoreCase = true)) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.confirmation)
                .setMessage(R.string.do_upload_video)
                .setPositiveButton(R.string.button_yes) { _, _ -> doUpload() }
                .setNegativeButton(R.string.button_no, null)
                .show()
        }
    }
}