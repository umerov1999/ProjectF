package dev.ragnarok.fenrir.fragment.fave.favepages

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IFaveInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.FindAtWithContent
import dev.ragnarok.fenrir.util.Utils.SafeCallCheckInt
import dev.ragnarok.fenrir.util.Utils.findIndexById
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.safeCheck
import dev.ragnarok.fenrir.util.Utils.safeCountOf
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class FavePagesPresenter(accountId: Long, isUser: Boolean, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IFavePagesView>(accountId, savedInstanceState) {
    private val pages: MutableList<FavePage>
    private val faveInteractor: IFaveInteractor
    private val isUser: Boolean
    private val cacheDisposable = CompositeDisposable()
    private val actualDataDisposable = CompositeDisposable()
    private val searcher: FindPage
    private var sleepDataDisposable = Disposable.disposed()
    private var actualDataReceived = false
    private var endOfContent = false
    private var cacheLoadingNow = false
    private var actualDataLoading = false
    private var doLoadTabs = false
    private fun sleep_search(q: String?) {
        if (actualDataLoading || cacheLoadingNow) return
        sleepDataDisposable.dispose()
        if (q.isNullOrEmpty()) {
            searcher.cancel()
        } else {
            if (!searcher.isSearchMode) {
                searcher.insertCache(pages, pages.size)
            }
            sleepDataDisposable = Single.just(Any())
                .delay(WEB_SEARCH_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .fromIOToMain()
                .subscribe({ searcher.do_search(q) }) { t ->
                    onActualDataGetError(
                        t
                    )
                }
        }
    }

    fun fireSearchRequestChanged(q: String?) {
        sleep_search(q?.trim { it <= ' ' })
    }

    override fun onGuiCreated(viewHost: IFavePagesView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(pages)
    }

    private fun loadActualData(offset: Int) {
        actualDataLoading = true
        resolveRefreshingView()
        actualDataDisposable.add(faveInteractor.getPages(accountId, GET_COUNT, offset, isUser)
            .fromIOToMain()
            .subscribe({
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
        endOfContent = safeCountOf(data) < GET_COUNT
        actualDataReceived = true
        if (offset == 0) {
            pages.clear()
            pages.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val startSize = pages.size
            pages.addAll(data)
            view?.notifyDataAdded(
                startSize,
                data.size
            )
        }
        resolveRefreshingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        loadActualData(0)
    }

    internal fun resolveRefreshingView() {
        resumedView?.showRefreshing(
            actualDataLoading
        )
    }

    private fun loadAllCachedData() {
        cacheLoadingNow = true
        cacheDisposable.add(faveInteractor.getCachedPages(accountId, isUser)
            .fromIOToMain()
            .subscribe({ onCachedDataReceived(it) }) { t ->
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
        cacheDisposable.dispose()
        actualDataDisposable.dispose()
        sleepDataDisposable.dispose()
        super.onDestroyed()
    }

    fun fireScrollToEnd() {
        if (pages.nonNullNoEmpty() && actualDataReceived && !cacheLoadingNow && !actualDataLoading) {
            if (searcher.isSearchMode) {
                searcher.do_search()
            } else if (!endOfContent) {
                loadActualData(pages.size)
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
        appendDisposable(faveInteractor.removePage(accountId, owner.ownerId, isUser)
            .fromIOToMain()
            .subscribe({ onUserRemoved(accountId, owner.ownerId) }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    fun firePushFirst(owner: Owner) {
        appendDisposable(faveInteractor.pushFirst(accountId, owner.ownerId)
            .fromIOToMain()
            .subscribe({ fireRefresh() }) { t ->
                showError(getCauseIfRuntime(t))
            })
    }

    fun fireMention(owner: Owner) {
        view?.openMention(
            accountId,
            owner
        )
    }

    private inner class FindPage(disposable: CompositeDisposable) : FindAtWithContent<FavePage>(
        disposable, SEARCH_VIEW_COUNT, SEARCH_COUNT
    ) {
        override fun search(offset: Int, count: Int): Single<List<FavePage>> {
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
                data.owner?.fullName,
                object : SafeCallCheckInt {
                    override fun check(): Boolean {
                        return data.owner?.fullName?.lowercase(Locale.getDefault())?.contains(
                            q.lowercase(
                                Locale.getDefault()
                            )
                        ) == true
                    }
                })
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
        pages = ArrayList()
        faveInteractor = InteractorFactory.createFaveInteractor()
        this.isUser = isUser
        searcher = FindPage(actualDataDisposable)
        loadAllCachedData()
    }
}