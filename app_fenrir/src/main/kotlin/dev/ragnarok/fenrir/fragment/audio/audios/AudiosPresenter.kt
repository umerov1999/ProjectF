package dev.ragnarok.fenrir.fragment.audio.audios

import android.content.Context
import android.os.Bundle
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.swap
import dev.ragnarok.fenrir.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.fenrir.util.FindAtWithContent
import dev.ragnarok.fenrir.util.HelperSimple
import dev.ragnarok.fenrir.util.HelperSimple.hasHelp
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCheck
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class AudiosPresenter(
    accountId: Long,
    private val ownerId: Long,
    albumId: Int?,
    private val accessKey: String?,
    private val iSSelectMode: Boolean,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IAudiosView>(accountId, savedInstanceState) {
    private val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    private val audios: ArrayList<Audio> = ArrayList()
    val playlistId: Int? = albumId
    private val audioListDisposable = CompositeJob()
    private val searcher: FindAudio = FindAudio(compositeJob)
    private var sleepDataDisposable = CancelableJob()
    private var swapDisposable = CancelableJob()
    private var actualReceived = false
    private var Curr: MutableList<AudioPlaylist>? = null
    private var loadingNow = false
    private var endOfContent = false
    private var doAudioLoadTabs = false
    private var needDeadHelper: Boolean
    private fun loadedPlaylist(t: AudioPlaylist) {
        val ret: MutableList<AudioPlaylist> = ArrayList(1)
        ret.add(t)
        view?.updatePlaylists(ret)
        Curr = ret
    }

    val isMyAudio: Boolean
        get() = playlistId == null && ownerId == accountId
    val isNotSearch: Boolean
        get() = !searcher.isSearchMode

    fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveRefreshingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doAudioLoadTabs = if (doAudioLoadTabs) {
            return
        } else {
            true
        }
        if (audios.isEmpty()) {
            if (!iSSelectMode && playlistId == null) {
                appendJob(
                    Includes.stores.tempStore().getAudiosAll(ownerId)
                        .fromIOToMain({
                            if (it.isEmpty()) {
                                fireRefresh()
                            } else {
                                audios.addAll(it)
                                actualReceived = true
                                setLoadingNow(false)
                                view?.notifyListChanged()
                            }
                        }, {
                            fireRefresh()
                        })
                )
            } else fireRefresh()
        }
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(loadingNow)
    }

    private fun requestNext() {
        setLoadingNow(true)
        val offset = audios.size
        requestList(offset, playlistId)
    }

    private fun requestList(offset: Int, album_id: Int?) {
        setLoadingNow(true)
        audioListDisposable.add(
            audioInteractor[accountId, album_id, ownerId, offset, GET_COUNT, accessKey]
                .fromIOToMain({
                    onListReceived(
                        offset,
                        it
                    )
                }) { t -> onListGetError(t) })
    }

    private fun onListReceived(offset: Int, data: List<Audio>) {
        endOfContent = data.isEmpty()
        actualReceived = true
        if (playlistId == null && !iSSelectMode) {
            appendJob(
                Includes.stores.tempStore().addAudios(ownerId, data, offset == 0)
                    .hiddenIO()
            )
        }
        if (offset == 0) {
            audios.clear()
            audios.addAll(data)
            view?.notifyListChanged()
        } else {
            val startOwnSize = audios.size
            audios.addAll(data)
            view?.notifyDataAdded(
                startOwnSize,
                data.size
            )
        }
        setLoadingNow(false)
        if (needDeadHelper) {
            for (i in audios) {
                if (i.url.isNullOrEmpty() || "https://vk.com/mp3/audio_api_unavailable.mp3" == i.url) {
                    needDeadHelper = false
                    view?.showAudioDeadHelper()
                    break
                }
            }
        }
    }

    fun playAudio(context: Context, position: Int) {
        startForPlayList(context, audios, position, false)
        if (!Settings.get().main().isShow_mini_player) getPlayerPlace(accountId).tryOpenWith(
            context
        )
    }

    fun fireDelete(position: Int) {
        audios.removeAt(position)
        view?.notifyItemRemoved(position)
    }

    override fun onDestroyed() {
        audioListDisposable.cancel()
        swapDisposable.cancel()
        sleepDataDisposable.cancel()
        super.onDestroyed()
    }

    internal fun onListGetError(t: Throwable) {
        setLoadingNow(false)
        showError(getCauseIfRuntime(t))
    }

    fun fireSelectAll() {
        for (i in audios) {
            i.isSelected = true
        }
        view?.notifyListChanged()
    }

    fun getSelected(noDownloaded: Boolean): ArrayList<Audio> {
        val ret = ArrayList<Audio>()
        for (i in audios) {
            if (i.isSelected) {
                if (noDownloaded) {
                    if (TrackIsDownloaded(i) == 0 && i.url
                            .nonNullNoEmpty() && !i.url!!.contains("file://") && !i.url!!.contains("content://")
                    ) {
                        ret.add(i)
                    }
                } else {
                    ret.add(i)
                }
            }
        }
        return ret
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

    fun fireUpdateSelectMode() {
        for (i in audios) {
            if (i.isSelected) {
                i.isSelected = false
            }
        }
        view?.notifyListChanged()
    }

    private fun sleep_search(q: String?) {
        if (loadingNow) return
        sleepDataDisposable.cancel()
        if (q.isNullOrEmpty()) {
            searcher.cancel()
        } else {
            if (!searcher.isSearchMode) {
                searcher.insertCache(audios, audios.size)
            }
            sleepDataDisposable += delayTaskFlow(WEB_SEARCH_DELAY.toLong())
                .toMain { searcher.do_search(q) }
        }
    }

    fun fireSearchRequestChanged(q: String?) {
        sleep_search(q?.trim())
    }

    fun fireRefresh() {
        if (searcher.isSearchMode) {
            searcher.reset()
        } else {
            if (playlistId != null && playlistId != 0) {
                audioListDisposable.add(
                    audioInteractor.getPlaylistById(
                        accountId,
                        playlistId,
                        ownerId,
                        accessKey
                    )
                        .fromIOToMain({ t -> loadedPlaylist(t) }) { t ->
                            showError(
                                getCauseIfRuntime(t)
                            )
                        })
            }
            requestList(0, playlistId)
        }
    }

    fun onDelete(album: AudioPlaylist) {
        audioListDisposable.add(
            audioInteractor.deletePlaylist(
                accountId,
                album.id,
                album.owner_id
            )
                .fromIOToMain({
                    view?.customToast?.showToast(
                        R.string.success
                    )
                }) { throwable ->
                    showError(throwable)
                })
    }

    fun onAdd(album: AudioPlaylist) {
        audioListDisposable.add(
            audioInteractor.followPlaylist(
                accountId,
                album.id,
                album.owner_id,
                album.access_key
            )
                .fromIOToMain({
                    view?.customToast?.showToast(R.string.success)
                }) { throwable ->
                    showError(throwable)
                })
    }

    fun fireScrollToEnd() {
        if (audios.nonNullNoEmpty() && !loadingNow && actualReceived) {
            if (searcher.isSearchMode) {
                searcher.do_search()
            } else if (!endOfContent) {
                requestNext()
            }
        }
    }

    fun fireEditTrack(context: Context, audio: Audio) {
        val root = View.inflate(context, R.layout.entry_audio_info, null)
        root.findViewById<TextInputEditText>(R.id.edit_artist).setText(audio.artist)
        root.findViewById<TextInputEditText>(R.id.edit_title).setText(audio.title)
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.enter_audio_info)
            .setCancelable(true)
            .setView(root)
            .setPositiveButton(R.string.button_ok) { _, _ ->
                audioListDisposable.add(
                    audioInteractor.edit(
                        accountId,
                        audio.ownerId,
                        audio.id,
                        root.findViewById<TextInputEditText>(R.id.edit_artist).text.toString(),
                        root.findViewById<TextInputEditText>(R.id.edit_title).text.toString()
                    ).fromIOToMain({ fireRefresh() }) { t ->
                        showError(getCauseIfRuntime(t))
                    })
            }
            .setNegativeButton(R.string.button_cancel, null)
            .show()
    }

    private fun tempSwap(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                audios.swap(i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                audios.swap(i, i - 1)
            }
        }
        view?.notifyItemMoved(
            fromPosition,
            toPosition
        )
    }

    fun fireItemMoved(fromPosition: Int, toPosition: Int): Boolean {
        if (audios.size < 2) {
            return false
        }
        val audio_from = audios[fromPosition]
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                audios.swap(i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                audios.swap(i, i - 1)
            }
        }
        view?.notifyItemMoved(
            fromPosition,
            toPosition
        )
        var before: Int? = null
        var after: Int? = null
        if (toPosition == 0) {
            before = audios[1].id
        } else {
            after = audios[toPosition - 1].id
        }
        swapDisposable.cancel()
        swapDisposable += audioInteractor.reorder(accountId, ownerId, audio_from.id, before, after)
            .fromIOToMain { tempSwap(toPosition, fromPosition) }
        return true
    }

    override fun onGuiCreated(viewHost: IAudiosView) {
        super.onGuiCreated(viewHost)
        viewHost.displayList(audios)
        Curr?.let {
            viewHost.updatePlaylists(it)
        }
    }

    private inner class FindAudio(disposable: CompositeJob) : FindAtWithContent<Audio>(
        disposable, SEARCH_VIEW_COUNT, SEARCH_COUNT
    ) {
        override fun search(offset: Int, count: Int): Flow<List<Audio>> {
            return audioInteractor[accountId, playlistId, ownerId, offset, count, accessKey]
        }

        override fun onError(e: Throwable) {
            onListGetError(e)
        }

        override fun onResult(data: MutableList<Audio>) {
            actualReceived = true
            val startSize = audios.size
            audios.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }

        override fun updateLoading(loading: Boolean) {
            setLoadingNow(loading)
        }

        override fun clean() {
            audios.clear()
            view?.notifyListChanged()
        }

        private fun checkArtists(data: Map<String, String>?, q: String): Boolean {
            if (data.isNullOrEmpty()) {
                return false
            }
            for (i in data.values) {
                if (i.lowercase(Locale.getDefault())
                        .contains(q.lowercase(Locale.getDefault()))
                ) {
                    return true
                }
            }
            return false
        }

        private fun checkTitleArtists(data: Audio, q: String): Boolean {
            val r = q.split(Regex("( - )|( )|( {2})"), 2).toTypedArray()
            return if (r.size >= 2) {
                (safeCheck(
                    data.artist
                ) {
                    data.artist?.lowercase(Locale.getDefault())?.contains(
                        r[0].lowercase(
                            Locale.getDefault()
                        )
                    ) == true
                }
                        || checkArtists(
                    data.main_artists,
                    r[0]
                )) && safeCheck(
                    data.title
                ) {
                    data.title?.lowercase(Locale.getDefault())?.contains(
                        r[1].lowercase(
                            Locale.getDefault()
                        )
                    ) == true
                }
            } else false
        }

        override fun compare(data: Audio, q: String): Boolean {
            return if (q == "dw") {
                TrackIsDownloaded(data) == 0
            } else safeCheck(
                data.title
            ) {
                data.title?.lowercase(Locale.getDefault())
                    ?.contains(q.lowercase(Locale.getDefault())) == true
            }
                    || safeCheck(
                data.artist
            ) {
                data.artist?.lowercase(Locale.getDefault())?.contains(
                    q.lowercase(
                        Locale.getDefault()
                    )
                ) == true
            }
                    || checkArtists(data.main_artists, q) || checkTitleArtists(data, q)
        }

        override fun onReset(data: MutableList<Audio>, offset: Int, isEnd: Boolean) {
            if (data.isEmpty()) {
                fireRefresh()
            } else {
                endOfContent = isEnd
                audios.clear()
                audios.addAll(data)
                if (playlistId == null && !iSSelectMode) {
                    appendJob(
                        Includes.stores.tempStore().addAudios(ownerId, data, true)
                            .hiddenIO()
                    )
                }
                view?.notifyListChanged()
            }
        }
    }

    companion object {
        private const val GET_COUNT = 100
        private const val SEARCH_COUNT = 1000
        private const val SEARCH_VIEW_COUNT = 20
        private const val WEB_SEARCH_DELAY = 1000
    }

    init {
        needDeadHelper = hasHelp(HelperSimple.AUDIO_DEAD, 1)
    }
}