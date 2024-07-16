package dev.ragnarok.filegallery.fragment.localserver.videoslocalserver

import dev.ragnarok.filegallery.Includes.networkInterfaces
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.fragment.base.RxSupportPresenter
import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.util.FindAt
import dev.ragnarok.filegallery.util.Utils
import dev.ragnarok.filegallery.util.coroutines.CancelableJob
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.filegallery.util.coroutines.CoroutinesUtils.toMain

class VideosLocalServerPresenter :
    RxSupportPresenter<IVideosLocalServerView>() {
    private val videos: MutableList<Video> = ArrayList()
    private val fInteractor: ILocalServerApi = networkInterfaces.localServerApi()
    private var actualDataDisposable = CancelableJob()
    private var Foffset = 0
    private var actualDataReceived = false
    private var endOfContent = false
    private var actualDataLoading = false
    private var search_at: FindAt
    private var reverse = false
    private var doLoadTabs = false
    override fun onGuiCreated(viewHost: IVideosLocalServerView) {
        super.onGuiCreated(viewHost)
        viewHost.displayList(videos)
    }

    fun toggleReverse() {
        reverse = !reverse
        fireRefresh(false)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        appendJob(fInteractor.getVideos(offset, GET_COUNT, reverse)
            .fromIOToMain({
                onActualDataReceived(
                    offset,
                    it
                )
            }) { onActualDataGetError(it) })
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

    private fun onActualDataReceived(offset: Int, data: List<Video>) {
        Foffset = offset + GET_COUNT
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        if (offset == 0) {
            videos.clear()
            videos.addAll(data)
            view?.notifyListChanged()
        } else {
            val startSize = videos.size
            videos.addAll(data)
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
        doLoadTabs = if (doLoadTabs) {
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
        if (!endOfContent && videos.nonNullNoEmpty() && actualDataReceived && !actualDataLoading) {
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
        appendJob(fInteractor.searchVideos(
            search_at.getQuery(),
            search_at.getOffset(),
            SEARCH_COUNT,
            reverse
        )
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

    private fun onSearched(search_at: FindAt, data: List<Video>) {
        actualDataLoading = false
        actualDataReceived = true
        endOfContent = search_at.isEnded()
        if (this.search_at.getOffset() == 0) {
            videos.clear()
            videos.addAll(data)
            view?.notifyListChanged()
        } else {
            if (data.nonNullNoEmpty()) {
                val startSize = videos.size
                videos.addAll(data)
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
        val query = q?.trim { it <= ' ' }
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