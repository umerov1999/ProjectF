package dev.ragnarok.fenrir.fragment.friends.allfriends

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IRelationshipInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UsersPart
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedIsNullOrEmpty
import dev.ragnarok.fenrir.util.Objects.safeEquals
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.indexOf
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.util.*
import java.util.concurrent.TimeUnit

class AllFriendsPresenter(accountId: Long, private val userId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<IAllFriendsView>(accountId, savedInstanceState) {
    private val relationshipInteractor: IRelationshipInteractor =
        InteractorFactory.createRelationshipInteractor()
    private val data: ArrayList<UsersPart> = ArrayList(3)
    private val actualDataDisposable = CompositeDisposable()
    private val cacheDisposable = CompositeDisposable()
    private val searchDisposable = CompositeDisposable()
    private val isNotFriendShow: Boolean
    private var q: String? = null
    private var actualDataReceived = false
    private var actualDataEndOfContent = false
    private var actualDataLoadingNow = false
    private var cacheLoadingNow = false
    private var searchRunNow = false
    private var doLoadTabs = false
    private var offset = 0
    public override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshingView()
        doLoadTabs = if (doLoadTabs) {
            return
        } else {
            true
        }
        loadAllCachedData()
        if (!isNotFriendShow) {
            requestActualData(false)
        }
    }

    private fun requestActualData(do_scan: Boolean) {
        actualDataLoadingNow = true
        resolveRefreshingView()
        actualDataDisposable.add(relationshipInteractor.getActualFriendsList(
            accountId,
            userId,
            if (isNotFriendShow) null else 200,
            offset
        )
            .fromIOToMain()
            .subscribe({ users ->
                onActualDataReceived(
                    users,
                    do_scan
                )
            }) { t -> onActualDataGetError(t) })
    }

    private fun onActualDataGetError(t: Throwable) {
        actualDataLoadingNow = false
        resolveRefreshingView()
        showError(getCauseIfRuntime(t))
    }

    override fun onGuiCreated(viewHost: IAllFriendsView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(data, isSearchNow)
        resolveSwipeRefreshAvailability()
    }

    private fun resolveRefreshingView() {
        view?.showRefreshing(!isSearchNow && actualDataLoadingNow)
    }

    private fun onActualDataReceived(users: List<User>, do_scan: Boolean) {
        if (do_scan && isNotFriendShow) {
            val not_friends = ArrayList<Owner>()
            val add_friends = ArrayList<Owner>()
            for (i in allData) {
                if (indexOf(users, i.getOwnerObjectId()) == -1) {
                    not_friends.add(i)
                }
            }
            if (userId != accountId) {
                for (i in users) {
                    if (indexOf(allData, i.getOwnerObjectId()) == -1) {
                        add_friends.add(i)
                    }
                }
            }
            if (add_friends.isNotEmpty() || not_friends.isNotEmpty()) {
                view?.showModFriends(
                    add_friends,
                    not_friends,
                    accountId, userId
                )
            }
        }
        // reset cache loading
        cacheDisposable.clear()
        cacheLoadingNow = false
        actualDataEndOfContent = users.isEmpty()
        actualDataReceived = true
        actualDataLoadingNow = false
        if (offset > 0) {
            val startSize = allData.size
            allData.addAll(users)
            if (!isSearchNow) {
                view?.notifyItemRangeInserted(
                    startSize,
                    users.size
                )
            }
        } else {
            allData.clear()
            allData.addAll(users)
            if (!isSearchNow) {
                safelyNotifyDataSetChanged()
            }
        }
        if (isNotFriendShow) {
            offset = allData.size
        } else {
            offset += 200
        }
        resolveRefreshingView()
    }

    private fun loadAllCachedData() {
        cacheLoadingNow = true
        cacheDisposable.add(relationshipInteractor.getCachedFriends(accountId, userId)
            .fromIOToMain()
            .subscribe({ users -> onCachedDataReceived(users) }) { t ->
                onCacheGetError(
                    t
                )
            })
    }

    private fun onCacheGetError(t: Throwable) {
        cacheLoadingNow = false
        showError(t)
        if (isNotFriendShow) {
            offset = 0
            requestActualData(false)
        }
    }

    private fun onCachedDataReceived(users: List<User>) {
        cacheLoadingNow = false
        allData.clear()
        allData.addAll(users)
        safelyNotifyDataSetChanged()
        if (isNotFriendShow) {
            offset = 0
            requestActualData(users.isNotEmpty())
        }
    }

    private fun safelyNotifyDataSetChanged() {
        view?.notifyDatasetChanged(isSearchNow)
    }

    private val allData: MutableList<User>
        get() = data[ALL].users

    fun fireRefresh() {
        if (!isSearchNow) {
            cacheDisposable.clear()
            actualDataDisposable.clear()
            cacheLoadingNow = false
            actualDataLoadingNow = false
            offset = 0
            requestActualData(false)
        }
    }

    private fun onSearchQueryChanged(searchStateChanged: Boolean) {
        searchDisposable.clear()
        if (searchStateChanged) {
            resolveSwipeRefreshAvailability()
        }
        if (!isSearchNow) {
            data[ALL].enable = true
            data[SEARCH_WEB].users.clear()
            data[SEARCH_WEB].enable = false
            data[SEARCH_WEB].displayCount = null
            data[SEARCH_CACHE].users.clear()
            data[SEARCH_CACHE].enable = false
            view?.notifyDatasetChanged(
                false
            )
            return
        }
        data[ALL].enable = false
        reFillCache()
        data[SEARCH_CACHE].enable = true
        data[SEARCH_WEB].users.clear()
        data[SEARCH_WEB].enable = true
        data[SEARCH_WEB].displayCount = null
        view?.notifyDatasetChanged(
            true
        )
        runNetSearch(0, true)
    }

    private fun runNetSearch(offset: Int, withDelay: Boolean) {
        if (q.trimmedIsNullOrEmpty()) {
            return
        }
        searchDisposable.clear()
        searchRunNow = true
        val query = q
        val single: Single<Pair<List<User>, Int>>
        val netSingle = relationshipInteractor.searchFriends(
            accountId,
            userId,
            WEB_SEARCH_COUNT_PER_LOAD,
            offset,
            query
        )
        single = if (withDelay) {
            Single.just(Any())
                .delay(WEB_SEARCH_DELAY.toLong(), TimeUnit.MILLISECONDS)
                .flatMap { netSingle }
        } else {
            netSingle
        }
        searchDisposable.add(single
            .fromIOToMain()
            .subscribe({
                onSearchDataReceived(
                    offset,
                    it.first,
                    it.second
                )
            }) { t -> onSearchError(t) })
    }

    private fun onSearchError(t: Throwable) {
        searchRunNow = false
        showError(getCauseIfRuntime(t))
    }

    private fun onSearchDataReceived(offset: Int, users: List<User>, fullCount: Int) {
        searchRunNow = false
        val searchData: MutableList<User> = data[SEARCH_WEB].users
        data[SEARCH_WEB].displayCount = fullCount
        if (offset == 0) {
            searchData.clear()
            searchData.addAll(users)
            view?.notifyDatasetChanged(
                isSearchNow
            )
        } else {
            val sizeBefore = searchData.size
            val currentCacheSize = data[SEARCH_CACHE].users.size
            searchData.addAll(users)
            view?.notifyItemRangeInserted(
                sizeBefore + currentCacheSize,
                users.size
            )
        }
    }

    private fun reFillCache() {
        data[SEARCH_CACHE].users.clear()
        val db: List<User> = data[ALL].users
        val preparedQ = q?.lowercase(Locale.getDefault())?.trim { it <= ' ' } ?: ""
        var count = 0
        for (user in db) {
            if (allow(user, preparedQ)) {
                data[SEARCH_CACHE].users.add(user)
                count++
            }
        }
        data[SEARCH_CACHE].displayCount = count
    }

    private val isSearchNow: Boolean
        get() = q.nonNullNoEmpty()

    private fun resolveSwipeRefreshAvailability() {
        view?.setSwipeRefreshEnabled(!isSearchNow)
    }

    fun fireSearchRequestChanged(q: String?) {
        val query = q?.trim { it <= ' ' }
        if (safeEquals(query, this.q)) {
            return
        }
        val wasSearch = isSearchNow
        this.q = query
        onSearchQueryChanged(wasSearch != isSearchNow)
    }

    override fun onDestroyed() {
        searchDisposable.dispose()
        cacheDisposable.dispose()
        actualDataDisposable.dispose()
        super.onDestroyed()
    }

    private fun loadMore() {
        if (isSearchNow) {
            if (searchRunNow) {
                return
            }
            runNetSearch(data[SEARCH_WEB].users.size, false)
        } else {
            if (actualDataLoadingNow || cacheLoadingNow || !actualDataReceived || actualDataEndOfContent) {
                return
            }
            requestActualData(false)
        }
    }

    fun fireScrollToEnd() {
        loadMore()
    }

    fun fireUserClick(user: User) {
        view?.showUserWall(
            accountId,
            user
        )
    }

    companion object {
        private const val ALL = 0
        private const val SEARCH_CACHE = 1
        private const val SEARCH_WEB = 2
        private const val WEB_SEARCH_DELAY = 1000
        private const val WEB_SEARCH_COUNT_PER_LOAD = 100
        internal fun allow(user: User, preparedQ: String): Boolean {
            val full = user.fullName.lowercase(Locale.getDefault())
            return full.contains(preparedQ)
        }
    }

    init {
        data.add(ALL, UsersPart(R.string.all_friends, ArrayList(), true))
        data.add(SEARCH_CACHE, UsersPart(R.string.results_in_the_cache, ArrayList(), false))
        data.add(SEARCH_WEB, UsersPart(R.string.results_in_a_network, ArrayList(), false))
        isNotFriendShow = Settings.get().other().isOwnerInChangesMonitor(userId)
    }
}