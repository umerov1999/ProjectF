package dev.ragnarok.fenrir.fragment.audio.audiosrecommendation

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.DownloadWorkUtils.TrackIsDownloaded
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.disposables.CompositeDisposable

class AudiosRecommendationPresenter(
    accountId: Int,
    private val ownerId: Int,
    private val top: Boolean,
    private val option_menu_id: Int,
    savedInstanceState: Bundle?
) : AccountDependencyPresenter<IAudiosRecommendationView>(accountId, savedInstanceState) {
    private val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    private val audios: ArrayList<Audio> = ArrayList()
    private val audioListDisposable = CompositeDisposable()
    private var loadingNow = false
    private var doAudioLoadTabs = false
    fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doAudioLoadTabs = if (doAudioLoadTabs) {
            return
        } else {
            true
        }
        fireRefresh()
    }

    private fun resolveRefreshingView() {
        resumedView?.displayRefreshing(
            loadingNow
        )
    }

    private fun onEndlessListReceived(data: List<Audio>) {
        audios.clear()
        audios.addAll(data)
        setLoadingNow(false)
        view?.notifyListChanged()
    }

    fun playAudio(context: Context, position: Int) {
        startForPlayList(context, audios, position, false)
        if (!Settings.get().other().isShow_mini_player) getPlayerPlace(accountId).tryOpenWith(
            context
        )
    }

    fun fireDelete(position: Int) {
        audios.removeAt(position)
        view?.notifyItemRemoved(
            position
        )
    }

    private fun getListByGenre(foreign: Boolean, genre: Int) {
        setLoadingNow(true)
        audioListDisposable.add(audioInteractor.getPopular(
            accountId,
            if (foreign) 1 else 0,
            genre,
            REC_COUNT
        )
            .fromIOToMain()
            .subscribe({ onEndlessListReceived(it) }) { t ->
                onListGetError(
                    t
                )
            })
    }

    private val recommendations: Unit
        get() {
            setLoadingNow(true)
            if (option_menu_id != 0) {
                audioListDisposable.add(audioInteractor.getRecommendationsByAudio(
                    accountId,
                    ownerId.toString() + "_" + option_menu_id,
                    REC_COUNT
                )
                    .fromIOToMain()
                    .subscribe({ onEndlessListReceived(it) }) {
                        onListGetError(
                            it
                        )
                    })
            } else {
                audioListDisposable.add(audioInteractor.getRecommendations(
                    accountId,
                    ownerId,
                    REC_COUNT
                )
                    .fromIOToMain()
                    .subscribe({ onEndlessListReceived(it) }) { t ->
                        onListGetError(
                            t
                        )
                    })
            }
        }

    override fun onDestroyed() {
        audioListDisposable.dispose()
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

    fun fireRefresh() {
        audioListDisposable.clear()
        if (top) {
            getListByGenre(false, option_menu_id)
        } else {
            recommendations
        }
    }

    override fun onGuiCreated(viewHost: IAudiosRecommendationView) {
        super.onGuiCreated(viewHost)
        viewHost.displayList(audios)
    }

    companion object {
        private const val REC_COUNT = 1000
    }

}