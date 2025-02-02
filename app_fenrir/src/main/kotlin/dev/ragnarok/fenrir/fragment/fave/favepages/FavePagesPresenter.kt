package dev.ragnarok.fenrir.fragment.fave.favepages

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.FindAtWithContent
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCheck
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import kotlin.math.abs

class FavePagesPresenter(
    accountId: Long,
    private val isUser: Boolean,
    savedInstanceState: Bundle?
) :
    AccountDependencyPresenter<IFavePagesView>(accountId, savedInstanceState) {
    private val pages: MutableList<FavePage> = ArrayList()
    private val faveInteractor: IFaveInteractor = InteractorFactory.createFaveInteractor()
    private val cacheDisposable = CompositeJob()
    private val actualDataDisposable = CompositeJob()
    private val searcher: FindPage = FindPage(actualDataDisposable)
    private var sleepDataDisposable = CancelableJob()
    private var actualDataReceived = false
    private var endOfContent = false
    private var cacheLoadingNow = false
    private var actualDataLoading = false
    private var doLoadTabs = false
    private var offsetPos = 0
    private fun sleep_search(q: String?) {
        if (actualDataLoading || cacheLoadingNow) return
        sleepDataDisposable.cancel()
        if (q.isNullOrEmpty()) {
            searcher.cancel()
        } else {
            if (!searcher.isSearchMode) {
                searcher.insertCache(pages, pages.size)
            }
            sleepDataDisposable += delayTaskFlow(WEB_SEARCH_DELAY.toLong())
                .toMain { searcher.do_search(q) }
        }
    }

    fun fireSearchRequestChanged(q: String?) {
        sleep_search(q?.trim())
    }

    override fun onGuiCreated(viewHost: IFavePagesView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(pages)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(
            faveInteractor.getPages(accountId, GET_COUNT, offset, isUser)
                .fromIOToMain({
                    onActualDataReceived(
                        offset,
                        it
                    )
                }) { t -> onActualDataGetError(t) })
    }

    internal fun onActualDataGetError(t: Throwable) {
        actualDataLoading = false
        showError(getCauseIfRuntime(t))
        resolveRefreshingView()
    }

    private fun onActualDataReceived(offset: Int, data: List<FavePage>) {
        cacheDisposable.clear()
        cacheLoadingNow = false
        actualDataLoading = false
        endOfContent = data.isEmpty()
        actualDataReceived = true
        offsetPos += GET_COUNT
        if (offset == 0) {
            pages.clear()
            pages.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val tmp = Utils.stripEqualsWithCounter(data, pages, GET_COUNT)
            if (tmp.isEmpty()) {
                endOfContent = true
            } else {
                val startSize = pages.size
                pages.addAll(tmp)
                view?.notifyDataAdded(
                    startSize,
                    tmp.size
                )
            }
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
        offsetPos = 0
        loadActualData(0)
    }

    internal fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
    }

    private fun loadAllCachedData() {
        cacheLoadingNow = true
        cacheDisposable.add(
            faveInteractor.getCachedPages(accountId, isUser)
                .fromIOToMain({ onCachedDataReceived(it) }) { t ->
                    onCachedGetError(
                        t
                    )
                })
    }

    private fun onCachedGetError(t: Throwable) {
        showError(getCauseIfRuntime(t))
    }

    private fun onCachedDataReceived(data: List<FavePage>) {
        cacheLoadingNow = false
        pages.clear()
        pages.addAll(data)
        view?.notifyDataSetChanged()
    }

    override fun onDestroyed() {
        cacheDisposable.cancel()
        actualDataDisposable.cancel()
        sleepDataDisposable.cancel()
        super.onDestroyed()
    }

    fun fireScrollToEnd() {
        if (pages.nonNullNoEmpty() && actualDataReceived && !cacheLoadingNow && !actualDataLoading) {
            if (searcher.isSearchMode) {
                searcher.do_search()
            } else if (!endOfContent) {
                loadActualData(offsetPos)
            }
        }
    }

    fun fireRefresh() {
        if (actualDataLoading || cacheLoadingNow) {
            return
        }
        if (searcher.isSearchMode) {
            searcher.reset()
        } else {
            offsetPos = 0
            loadActualData(0)
        }
    }

    fun fireOwnerClick(owner: Owner) {
        view?.openOwnerWall(
            accountId,
            owner
        )
    }

    private fun onUserRemoved(accountId: Long, ownerId: Long) {
        if (accountId != ownerId) {
            return
        }
        val index = findIndexById(pages, abs(ownerId))
        if (index != -1) {
            pages.removeAt(index)
            view?.notifyItemRemoved(
                index
            )
        }
    }

    fun fireOwnerDelete(owner: Owner) {
        appendJob(
            faveInteractor.removePage(accountId, owner.ownerId, isUser)
                .fromIOToMain({ onUserRemoved(accountId, owner.ownerId) }) { t ->
                    showError(getCauseIfRuntime(t))
                })
    }

    fun firePushFirst(owner: Owner) {
        appendJob(
            faveInteractor.pushFirst(accountId, owner.ownerId)
                .fromIOToMain({ fireRefresh() }) { t ->
                    showError(getCauseIfRuntime(t))
                })
    }

    fun fireMention(owner: Owner) {
        view?.openMention(
            accountId,
            owner
        )
    }

    private inner class FindPage(disposable: CompositeJob) : FindAtWithContent<FavePage>(
        disposable, SEARCH_VIEW_COUNT, SEARCH_COUNT
    ) {
        override fun search(offset: Int, count: Int): Flow<List<FavePage>> {
            return faveInteractor.getPages(accountId, count, offset, isUser)
        }

        override fun onError(e: Throwable) {
            onActualDataGetError(e)
        }

        override fun onResult(data: MutableList<FavePage>) {
            actualDataReceived = true
            val startSize = pages.size
            pages.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }

        override fun updateLoading(loading: Boolean) {
            actualDataLoading = loading
            resolveRefreshingView()
        }

        override fun clean() {
            pages.clear()
            view?.notifyDataSetChanged()
        }

        override fun compare(data: FavePage, q: String): Boolean {
            return data.owner != null && safeCheck(
                data.owner?.fullName
            ) {
                data.owner?.fullName?.lowercase(Locale.getDefault())?.contains(
                    q.lowercase(
                        Locale.getDefault()
                    )
                ) == true
            }
        }

        override fun onReset(data: MutableList<FavePage>, offset: Int, isEnd: Boolean) {
            if (data.isEmpty()) {
                fireRefresh()
            } else {
                pages.clear()
                pages.addAll(data)
                endOfContent = isEnd
                view?.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private const val SEARCH_COUNT = 250
        private const val SEARCH_VIEW_COUNT = 20
        private const val GET_COUNT = 500
        private const val WEB_SEARCH_DELAY = 1000
    }

    init {
        loadAllCachedData()
    }
}