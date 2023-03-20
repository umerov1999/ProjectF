package dev.ragnarok.fenrir.fragment.audio.local.audioslocal

import android.content.Context
import android.net.Uri
import android.os.Bundle
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.db.Stores
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.upload.IUploadManager
import dev.ragnarok.fenrir.upload.IUploadManager.IProgressUpdate
import dev.ragnarok.fenrir.upload.Upload
import dev.ragnarok.fenrir.upload.UploadDestination
import dev.ragnarok.fenrir.upload.UploadDestination.Companion.forAudio
import dev.ragnarok.fenrir.upload.UploadDestination.Companion.forRemotePlay
import dev.ragnarok.fenrir.upload.UploadIntent
import dev.ragnarok.fenrir.upload.UploadResult
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.SafeCallCheckInt
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCheck
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.Locale

class AudiosLocalPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IAudiosLocalView>(accountId, savedInstanceState) {
    private val origin_audios: ArrayList<Audio>
    private val audios: ArrayList<Audio>
    private val audioListDisposable = CompositeDisposable()
    private val uploadManager: IUploadManager = Includes.uploadManager
    private val uploadsData: MutableList<Upload>
    private val destination: UploadDestination = forAudio(accountId)
    private val remotePlay: UploadDestination = forRemotePlay()
    private var actualReceived = false
    private var loadingNow = false
    private var query: String? = null
    private var errorPermissions = false
    private var doAudioLoadTabs = false
    private var bucket_id = 0
    fun fireBucketSelected(bucket_id: Int) {
        this.bucket_id = bucket_id
        fireRefresh()
    }

    fun firePrepared() {
        appendDisposable(uploadManager[accountId, destination]
            .fromIOToMain()
            .subscribe { data -> onUploadsDataReceived(data) })
        appendDisposable(uploadManager.observeAdding()
            .observeOn(provideMainThreadScheduler())
            .subscribe { added -> onUploadsAdded(added) })
        appendDisposable(uploadManager.observeDeleting(true)
            .observeOn(provideMainThreadScheduler())
            .subscribe { ids -> onUploadDeleted(ids) })
        appendDisposable(uploadManager.observeResults()
            .filter {
                destination.compareTo(
                    it.first.destination
                ) || remotePlay.compareTo(it.first.destination)
            }
            .observeOn(provideMainThreadScheduler())
            .subscribe { pair -> onUploadResults(pair) })
        appendDisposable(uploadManager.observeStatus()
            .observeOn(provideMainThreadScheduler())
            .subscribe { upload -> onUploadStatusUpdate(upload) })
        appendDisposable(uploadManager.observeProgress()
            .observeOn(provideMainThreadScheduler())
            .subscribe { updates -> onProgressUpdates(updates) })
        fireRefresh()
    }

    fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveRefreshingView()
    }

    private fun checkTittleArtists(data: Audio, q: String): Boolean {
        val r = q.split(Regex("( - )|( )|( {2})"), 2).toTypedArray()
        return if (r.size >= 2) {
            (safeCheck(
                data.artist,
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.artist?.lowercase(Locale.getDefault())?.contains(
                            r[0].lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                })
                    && safeCheck(
                data.title,
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.title?.lowercase(Locale.getDefault())?.contains(
                            r[1].lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                }))
        } else false
    }

    private fun updateCriteria() {
        setLoadingNow(true)
        audios.clear()
        if (query.isNullOrEmpty()) {
            audios.addAll(origin_audios)
            setLoadingNow(false)
            view?.notifyListChanged()
            return
        }
        query?.let {
            for (i in origin_audios) {
                if (safeCheck(i.title, object : SafeCallCheckInt {
                        override fun check(): Boolean {
                            return i.title?.lowercase(Locale.getDefault())?.contains(
                                it.lowercase(Locale.getDefault())
                            ) == true
                        }
                    })
                    || safeCheck(i.artist, object : SafeCallCheckInt {
                        override fun check(): Boolean {
                            return i.artist?.lowercase(Locale.getDefault())?.contains(
                                it.lowercase(Locale.getDefault())
                            ) == true
                        }
                    }) || checkTittleArtists(i, it)
                ) {
                    audios.add(i)
                }
            }
        }
        setLoadingNow(false)
        view?.notifyListChanged()
    }

    fun fireQuery(q: String?) {
        query = if (q.isNullOrEmpty()) null else {
            q
        }
        updateCriteria()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doAudioLoadTabs = if (doAudioLoadTabs) {
            return
        } else {
            true
        }
        view?.checkPermission()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            loadingNow
        )
    }

    fun requestList() {
        setLoadingNow(true)
        if (bucket_id == 0) {
            audioListDisposable.add(Stores.instance
                .localMedia()
                .getAudios(accountId)
                .fromIOToMain()
                .subscribe({ onListReceived(it) }) { t ->
                    onListGetError(
                        t
                    )
                })
        } else {
            audioListDisposable.add(Stores.instance
                .localMedia()
                .getAudios(accountId, bucket_id.toLong())
                .fromIOToMain()
                .subscribe({ onListReceived(it) }) { t ->
                    onListGetError(
                        t
                    )
                })
        }
    }

    private fun onListReceived(data: List<Audio>) {
        if (data.isEmpty()) {
            actualReceived = true
            setLoadingNow(false)
            return
        }
        origin_audios.clear()
        actualReceived = true
        origin_audios.addAll(data)
        updateCriteria()
        setLoadingNow(false)
    }

    fun playAudio(context: Context, position: Int) {
        startForPlayList(context, audios, position, false)
        if (!Settings.get().other().isShow_mini_player) getPlayerPlace(accountId).tryOpenWith(
            context
        )
    }

    fun fireDelete(position: Int) {
        audios.removeAt(position)
        view?.notifyItemRemoved(position)
    }

    override fun onDestroyed() {
        audioListDisposable.dispose()
        super.onDestroyed()
    }

    fun fireRemoveClick(upload: Upload) {
        uploadManager.cancel(upload.getObjectId())
    }

    private fun onListGetError(t: Throwable) {
        setLoadingNow(false)
        showError(
            getCauseIfRuntime(t)
        )
    }

    fun fireFileForUploadSelected(file: String?) {
        val intent = UploadIntent(accountId, destination)
            .setAutoCommit(true)
            .setFileUri(Uri.parse(file))
        uploadManager.enqueue(listOf(intent))
    }

    fun fireFileForRemotePlaySelected(file: String?) {
        val intent = UploadIntent(accountId, remotePlay)
            .setAutoCommit(true)
            .setFileUri(Uri.parse(file))
        uploadManager.enqueue(listOf(intent))
    }

    fun getAudioPos(audio: Audio?): Int {
        if (audios.isNotEmpty() && audio != null) {
            for ((pos, i) in audios.withIndex()) {
                if (i.id == audio.id && i.ownerId == audio.ownerId) {
                    i.isAnimationNow = true
                    view?.notifyItemChanged(
                        pos
                    )
                    return pos
                }
            }
        }
        return -1
    }

    fun firePermissionsCanceled() {
        errorPermissions = true
    }

    fun fireRefresh() {
        if (errorPermissions) {
            errorPermissions = false
            view?.checkPermission()
            return
        }
        audioListDisposable.clear()
        requestList()
    }

    fun fireScrollToEnd() {
        if (actualReceived) {
            requestList()
        }
    }

    override fun onGuiCreated(viewHost: IAudiosLocalView) {
        super.onGuiCreated(viewHost)
        viewHost.displayList(audios)
        viewHost.displayUploads(uploadsData)
        resolveUploadDataVisibility()
    }

    private fun onUploadsDataReceived(data: List<Upload>) {
        uploadsData.clear()
        uploadsData.addAll(data)
        resolveUploadDataVisibility()
    }

    private fun onUploadResults(pair: Pair<Upload, UploadResult<*>>) {
        val obj = pair.second.result as Audio
        if (obj.id == 0) view?.customToast?.showToastError(
            R.string.error
        )
        else {
            view?.customToast?.showToast(
                R.string.uploaded
            )
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

    init {
        uploadsData = ArrayList(0)
        audios = ArrayList()
        origin_audios = ArrayList()
    }
}