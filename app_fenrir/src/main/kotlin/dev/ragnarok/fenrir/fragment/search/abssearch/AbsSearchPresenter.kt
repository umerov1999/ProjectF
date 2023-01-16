package dev.ragnarok.fenrir.fragment.search.abssearch

import android.os.Bundle
import dev.ragnarok.fenrir.fragment.base.PlaceSupportPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.BaseSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.AbsNextFrom
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.WeakActionHandler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable

abstract class AbsSearchPresenter<V : IBaseSearchView<T>, C : BaseSearchCriteria, T, N : AbsNextFrom> internal constructor(
    accountId: Long,
    var criteria: C?,
    savedInstanceState: Bundle?
) : PlaceSupportPresenter<V>(accountId, savedInstanceState) {
    val data: MutableList<T> = ArrayList()
    private lateinit var actionHandler: WeakActionHandler<AbsSearchPresenter<*, *, *, *>>
    private val searchDisposable = CompositeDisposable()
    lateinit var nextFrom: N
        private set
    private var resultsForCriteria: C? = null
    private var endOfContent = false
    private var loadingNow = false
    override fun onGuiCreated(viewHost: V) {
        super.onGuiCreated(viewHost)

        // пробуем искать при первом создании view
        if (viewCreationCount == 1) {
            doSearch()
        }
        resolveListData()
        resolveEmptyText()
    }

    abstract val initialNextFrom: N
    override fun saveState(outState: Bundle) {
        super.saveState(outState)
        outState.putParcelable(SAVE_CRITERIA, criteria)
    }

    @Suppress("UNCHECKED_CAST")
    fun doSearch() {
        if (!canSearch(criteria)) {
            //setLoadingNow(false);
            return
        }
        val cloneCriteria = (criteria ?: return).safellyClone() as C
        val nf: N = nextFrom
        setLoadingNow(true)
        searchDisposable.add(
            doSearch(accountId, cloneCriteria, nf)
                .fromIOToMain()
                .subscribe({
                    onSearchDataReceived(
                        cloneCriteria,
                        nf,
                        it.first,
                        it.second
                    )
                }, { throwable -> onSearchError(throwable) })
        )
    }

    open fun onSearchError(throwable: Throwable) {
        throwable.printStackTrace()
    }

    abstract fun isAtLast(startFrom: N): Boolean
    private fun onSearchDataReceived(criteria: C, startFrom: N, data: List<T>, nextFrom: N) {
        setLoadingNow(false)
        val clearPrevious = isAtLast(startFrom)
        this.nextFrom = nextFrom
        resultsForCriteria = criteria
        endOfContent = data.isEmpty()
        if (clearPrevious) {
            this.data.clear()
            this.data.addAll(data)
            view?.notifyDataSetChanged()
        } else {
            val startSize = this.data.size
            this.data.addAll(data)
            view?.notifyDataAdded(startSize, data.size)
        }
        resolveEmptyText()
    }

    fun fireTextQueryEdit(q: String?) {
        criteria?.query = q
        fireCriteriaChanged()
    }

    private fun resolveListData() {
        view?.displayData(data)
    }

    private fun resolveEmptyText() {
        view?.setEmptyTextVisible(data.isEmpty())
    }

    private fun setLoadingNow(loadingNow: Boolean) {
        this.loadingNow = loadingNow
        resolveLoadingView()
    }

    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveLoadingView()
    }

    private fun resolveLoadingView() {
        view?.showLoading(loadingNow)
    }

    private fun fireCriteriaChanged() {
        if (criteria?.equals(resultsForCriteria) == true) {
            return
        }
        searchDisposable.clear()
        setLoadingNow(false)
        nextFrom = initialNextFrom
        data.clear()
        resolveListData()
        resolveEmptyText()
        actionHandler.removeMessages(MESSAGE)
        if (canSearch(criteria)) {
            actionHandler.sendEmptyMessageDelayed(MESSAGE, SEARCH_DELAY.toLong())
        }
    }

    abstract fun doSearch(accountId: Long, criteria: C, startFrom: N): Single<Pair<List<T>, N>>
    abstract fun instantiateEmptyCriteria(): C
    override fun onDestroyed() {
        actionHandler.setAction(null)
        searchDisposable.clear()
        super.onDestroyed()
    }

    abstract fun canSearch(criteria: C?): Boolean
    fun fireScrollToEnd() {
        if (canLoadMore()) {
            doSearch()
        }
    }

    private fun canLoadMore(): Boolean {
        return !endOfContent && !loadingNow && data.isNotEmpty()
    }

    fun fireRefresh() {
        if (loadingNow || !canSearch(criteria)) {
            resolveLoadingView()
            return
        }
        nextFrom = initialNextFrom
        doSearch()
    }

    fun fireOptionsChanged() {
        fireCriteriaChanged()
    }

    fun fireOpenFilterClick() {
        criteria?.let { view?.displayFilter(accountId, it.options) }
    }

    companion object {
        private const val SAVE_CRITERIA = "save_criteria"
        private const val MESSAGE = 67
        private const val SEARCH_DELAY = 1500
    }

    abstract fun readParcelSaved(savedInstanceState: Bundle, key: String): C?

    private fun create(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            this.criteria = criteria ?: instantiateEmptyCriteria()
        } else {
            this.criteria = readParcelSaved(savedInstanceState, SAVE_CRITERIA)
        }
        nextFrom = initialNextFrom
        actionHandler = WeakActionHandler(this)
        actionHandler.setAction(object : WeakActionHandler.Action<AbsSearchPresenter<*, *, *, *>> {
            override fun doAction(what: Int, orig: AbsSearchPresenter<*, *, *, *>) {
                orig.doSearch()
            }
        })
    }

    init {
        create(savedInstanceState)
    }
}