package dev.ragnarok.fenrir.fragment.audio.audiosbyartist

import android.content.Context
import android.os.Bundle
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
import dev.ragnarok.fenrir.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain

class AudiosByArtistPresenter(
    accountId: Long,
    private val artist: String,
    savedInstanceState: Bundle?
) :
    AccountDependencyPresenter<IAudiosByArtistView>(accountId, savedInstanceState) {
    private val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    private val audios: ArrayList<Audio> = ArrayList()
    private val audioListDisposable = CompositeJob()
    private var actualReceived = false
    private var loadingNow = false
    private var endOfContent = false
    val isMyAudio: Boolean
        get() = false

    fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveRefreshingView()
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            loadingNow
        )
    }

    private fun requestNext() {
        setLoadingNow(true)
        val offset = audios.size
        requestList(offset)
    }

    private fun requestList(offset: Int) {
        setLoadingNow(true)
        audioListDisposable.add(
            audioInteractor.getAudiosByArtist(
                accountId,
                artist,
                offset,
                GET_COUNT
            )
                .fromIOToMain({
                    if (offset == 0) {
                        onListReceived(it)
                    } else {
                        onNextListReceived(it)
                    }
                }) { t -> onListGetError(t) })
    }

    private fun onNextListReceived(next: List<Audio>) {
        val startOwnSize = audios.size
        audios.addAll(next)
        endOfContent = next.isEmpty()
        setLoadingNow(false)
        view?.notifyDataAdded(
            startOwnSize,
            next.size
        )
    }

    private fun onListReceived(data: List<Audio>) {
        audios.clear()
        audios.addAll(data)
        endOfContent = data.isEmpty()
        actualReceived = true
        setLoadingNow(false)
        view?.notifyListChanged()
    }

    fun playAudio(context: Context, position: Int) {
        startForPlayList(context, audios, position, false)
        if (!Settings.get().main().isShow_mini_player) getPlayerPlace(accountId).tryOpenWith(
            context
        )
    }

    override fun onDestroyed() {
        audioListDisposable.cancel()
        super.onDestroyed()
    }

    private fun onListGetError(t: Throwable) {
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
                    if (TrackIsDownloaded(i) == 0 && i.url.nonNullNoEmpty() && !i.url!!.contains("file://") && !i.url!!.contains(
                            "content://"
                        )
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

    fun fireRefresh() {
        audioListDisposable.clear()
        requestList(0)
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
                    view?.customToast?.showToast(
                        R.string.success
                    )
                }) { throwable ->
                    showError(
                        throwable
                    )
                })
    }

    fun fireScrollToEnd() {
        if (actualReceived && !endOfContent) {
            requestNext()
        }
    }

    override fun onGuiCreated(viewHost: IAudiosByArtistView) {
        super.onGuiCreated(viewHost)
        viewHost.displayList(audios)
    }

    companion object {
        private const val GET_COUNT = 100
    }

    init {
        fireRefresh()
    }
}