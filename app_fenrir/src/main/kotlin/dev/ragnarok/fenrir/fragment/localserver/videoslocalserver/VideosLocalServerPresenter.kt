package dev.ragnarok.fenrir.fragment.localserver.videoslocalserver

import android.os.Bundle
import dev.ragnarok.fenrir.domain.ILocalServerInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.FindAt
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain

class VideosLocalServerPresenter(accountId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IVideosLocalServerView>(accountId, savedInstanceState) {
    private val videos: MutableList<Video> = ArrayList()
    private val fInteractor: ILocalServerInteractor =
        InteractorFactory.createLocalServerInteractor()
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
        appendJob(
            fInteractor.getVideos(offset, GET_COUNT, reverse)
                .fromIOToMain({
                    onActualDataReceived(
                        offset,
                        it
                    )
                }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
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
        resumedView?.displayLoading(
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
        appendJob(
            fInteractor.searchVideos(
                search_at.getQuery(),
                search_at.getOffset(),
                SEARCH_COUNT,
                reverse
            )
                .fromIOToMain({
                    onSearched(
                        FindAt(
                            search_at.getQuery(),
                            search_at.getOffset() + SEARCH_COUNT,
                            it.size < SEARCH_COUNT
                        ), it
                    )
                }) { t -> onActualDataGetError(t) })
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
        actualDataDisposable += delayTaskFlow(WEB_SEARCH_DELAY.toLong())
            .toMain { doSearch() }
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