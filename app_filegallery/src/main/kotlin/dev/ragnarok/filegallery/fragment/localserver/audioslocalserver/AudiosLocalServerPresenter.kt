package dev.ragnarok.filegallery.fragment.localserver.audioslocalserver

import android.content.Context
import dev.ragnarok.filegallery.Includes.networkInterfaces
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.filegallery.settings.Settings.get
import dev.ragnarok.filegallery.util.FindAt
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.toMain

class AudiosLocalServerPresenter :
    RxSupportPresenter<IAudiosLocalServerView>() {
    private val audios: MutableList<Audio> = ArrayList()
    private val fInteractor: ILocalServerApi = networkInterfaces.localServerApi()
    private var actualDataDisposable = CancelableJob()
    private var Foffset = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    private var search_at: FindAt
    private var reverse = false
    private var discography = false
    private var doAudioLoadTabs = false
    fun updateReverse(reverse: Boolean) {
        this.reverse = reverse
        fireRefresh(false)
    }

    fun updateDiscography(discography: Boolean) {
        this.discography = discography
        if (search_at.isSearchMode()) {
            search_at.reset(true)
        }
        fireRefresh(false)
    }

    fun fireOptions() {
        view?.displayOptionsDialog(
            reverse,
            discography
        )
    }

    override fun onGuiCreated(viewHost: IAudiosLocalServerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayList(audios)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        appendJob((if (discography) fInteractor.getDiscography(
            offset,
            GET_COUNT,
            reverse
        ) else fInteractor.getAudios(offset, GET_COUNT, reverse))
            .fromIOToMain({
                onActualDataReceived(
                    offset,
                    it
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        view?.let {
            showError(
                it,
                Utils.getCauseIfRuntime(t)
            )
        }
        resolveRefreshingView()
    }

    private fun onActualDataReceived(offset: Int, data: List<Audio>) {
        Foffset = offset + GET_COUNT
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (offset == 0) {
            audios.clear()
            audios.addAll(data)
            view?.notifyListChanged()
        } else {
            val startSize = audios.size
            audios.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }
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
        loadActualData(0)
    }

    private fun resolveRefreshingView() {
        view?.displayLoading(
            actualDataLoading
        )
    }

    override fun onDestroyed() {
        actualDataDisposable.cancel()
        super.onDestroyed()
    }

    fun fireScrollToEnd(): Boolean {
        if (!endOfContent && audios.nonNullNoEmpty() && actualDataReceived && !actualDataLoading) {
            if (search_at.isSearchMode()) {
                search(false)
            } else {
                loadActualData(Foffset)
            }
            return false
        }
        return true
    }

    private fun doSearch() {
        actualDataLoading = true
        resolveRefreshingView()
        appendJob((if (discography) fInteractor.searchDiscography(
            search_at.getQuery(),
            search_at.getOffset(),
            SEARCH_COUNT,
            reverse
        ) else fInteractor.searchAudios(
            search_at.getQuery(),
            search_at.getOffset(),
            SEARCH_COUNT,
            reverse
        ))
            .fromIOToMain({
                onSearched(
                    FindAt(
                        search_at.getQuery() ?: return@fromIOToMain,
                        search_at.getOffset() + SEARCH_COUNT,
                        it.size < SEARCH_COUNT
                    ), it
                )
            }) { onActualDataGetError(it) })
    }

    private fun onSearched(search_at: FindAt, data: List<Audio>) {
        actualDataLoading = false
        actualDataReceived = true
        endOfContent = search_at.isEnded()
        if (this.search_at.getOffset() == 0) {
            audios.clear()
            audios.addAll(data)
            view?.notifyListChanged()
        } else {
            if (data.nonNullNoEmpty()) {
                val startSize = audios.size
                audios.addAll(data)
                view?.notifyDataAdded(
                    startSize,
                    data.size
                )
            }
        }
        this.search_at = search_at
        resolveRefreshingView()
    }

    private fun search(sleep_search: Boolean) {
        if (actualDataLoading) return
        if (!sleep_search) {
            doSearch()
            return
        }
        actualDataDisposable.cancel()
        actualDataDisposable.set(delayTaskFlow(WEB_SEARCH_DELAY.toLong())
            .toMain { doSearch() })
    }

    fun fireSearchRequestChanged(q: String?) {
        val query = q?.trim()
        if (!search_at.do_compare(query)) {
            actualDataLoading = false
            if (query.isNullOrEmpty()) {
                actualDataDisposable.cancel()
                fireRefresh(false)
            } else {
                fireRefresh(true)
            }
        }
    }

    fun getAudioPos(audio: Audio?): Int {
        if (audios.nonNullNoEmpty() && audio != null) {
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

    fun playAudio(context: Context, position: Int) {
        startForPlayList(context, ArrayList(audios), position, false)
        if (!get().main().isShow_mini_player) getPlayerPlace().tryOpenWith(
            context
        )
    }

    fun fireRefresh(sleep_search: Boolean) {
        if (actualDataLoading) {
            return
        }
        if (search_at.isSearchMode()) {
            search_at.reset(false)
            search(sleep_search)
        } else {
            loadActualData(0)
        }
    }

    companion object {
        private const val SEARCH_COUNT = 50
        private const val GET_COUNT = 100
        private const val WEB_SEARCH_DELAY = 1000
    }

    init {
        search_at = FindAt()
    }
}