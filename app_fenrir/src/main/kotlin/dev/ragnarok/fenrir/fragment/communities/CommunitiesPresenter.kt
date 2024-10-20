package dev.ragnarok.fenrir.fragment.communities

import android.os.Bundle
import dev.ragnarok.fenrir.domain.ICommunitiesInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.base.AccountDependencyPresenter
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.model.DataWrapper
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedIsNullOrEmpty
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Objects.safeEquals
import dev.ragnarok.fenrir.util.Translit.cyr2lat
import dev.ragnarok.fenrir.util.Translit.lat2cyr
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.Utils.indexOf
import dev.ragnarok.fenrir.util.coroutines.CompositeJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.andThen
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class CommunitiesPresenter(accountId: Long, private val userId: Long, savedInstanceState: Bundle?) :
    AccountDependencyPresenter<ICommunitiesView>(accountId, savedInstanceState) {
    private val own: DataWrapper<Community> = DataWrapper(ArrayList(), true)
    private val filtered: DataWrapper<Community> = DataWrapper(ArrayList(0), false)
    private val search: DataWrapper<Community> = DataWrapper(ArrayList(0), false)
    private val communitiesInteractor: ICommunitiesInteractor =
        InteractorFactory.createCommunitiesInteractor()
    private val actualDisposable = CompositeJob()
    private val cacheDisposable = CompositeJob()
    private val netSearchDisposable = CompositeJob()
    private val filterDisposable = CompositeJob()
    private val isNotFriendShow: Boolean =
        Settings.get().main().isOwnerInChangesMonitor(userId) && userId != accountId
    private var actualEndOfContent = false
    private var netSearchEndOfContent = false
    private var actualLoadingNow = false

    private var cacheLoadingNow = false
    private var netSearchNow = false
    private var filter: String? = null

    private var offset = 0
    private var networkOffset = 0
    private fun requestActualData(do_scan: Boolean) {
        actualLoadingNow = true
        resolveRefreshing()
        actualDisposable.add(communitiesInteractor.getActual(
            accountId,
            userId,
            if (isNotFriendShow) 1000 else 200,
            offset, true
        )
            .fromIOToMain({ communities ->
                onActualDataReceived(
                    communities,
                    do_scan
                )
            }) { t -> onActualDataGetError(t) })
    }

    override fun onGuiResumed() {
        super.onGuiResumed()
        resolveRefreshing()
    }

    private fun resolveRefreshing() {
        resumedView?.displayRefreshing(
            actualLoadingNow || netSearchNow
        )
    }

    private fun onActualDataGetError(t: Throwable) {
        actualLoadingNow = false
        resolveRefreshing()
        showError(t)
    }

    override fun onGuiCreated(viewHost: ICommunitiesView) {
        super.onGuiCreated(viewHost)
        viewHost.displayData(own, filtered, search)
    }

    private fun onActualDataReceived(communities: List<Community>, do_scan: Boolean) {
        //reset cache loading
        cacheDisposable.clear()
        cacheLoadingNow = false
        actualLoadingNow = false
        actualEndOfContent = communities.isEmpty()
        if (do_scan && isNotFriendShow) {
            val not_communities = ArrayList<Owner>()
            for (i in own.data) {
                if (indexOf(communities, i.ownerId) == -1) {
                    not_communities.add(i)
                }
            }
            val add_communities = ArrayList<Owner>()
            for (i in communities) {
                if (indexOf(own.data, i.ownerId) == -1) {
                    add_communities.add(i)
                }
            }
            if (add_communities.isNotEmpty() || not_communities.isNotEmpty()) {
                view?.showModCommunities(
                    add_communities,
                    not_communities,
                    accountId, userId
                )
            }
        }
        if (offset == 0) {
            own.data.clear()
            own.data.addAll(communities)
            view?.notifyDataSetChanged()
        } else {
            val startOwnSize = own.size()
            own.data.addAll(communities)
            view?.notifyOwnDataAdded(
                startOwnSize,
                communities.size
            )
        }
        offset += if (isNotFriendShow) 1000 else 200
        resolveRefreshing()
    }

    private fun loadCachedData() {
        cacheLoadingNow = true
        cacheDisposable.add(communitiesInteractor.getCachedData(accountId, userId)
            .fromIOToMain({ communities -> onCachedDataReceived(communities) }) { t ->
                onCacheGetError(
                    t
                )
            })
    }

    private val isSearchNow: Boolean
        get() = filter.trimmedNonNullNoEmpty()

    private fun onCacheGetError(t: Throwable) {
        cacheLoadingNow = false
        showError(t)
        if (isNotFriendShow) {
            requestActualData(false)
        }
    }

    private fun onCachedDataReceived(communities: List<Community>) {
        cacheLoadingNow = false
        own.data.clear()
        own.data.addAll(communities)
        view?.notifyDataSetChanged()
        if (isNotFriendShow) {
            requestActualData(communities.isNotEmpty())
        }
    }

    fun fireSearchQueryChanged(query: String?) {
        if (!safeEquals(filter, query)) {
            filter = query
            onFilterChanged()
        }
    }

    private fun onFilterChanged() {
        val searchNow = filter.trimmedNonNullNoEmpty()
        own.isEnabled = !searchNow
        filtered.isEnabled = searchNow
        filtered.clear()
        search.isEnabled = searchNow
        search.clear()
        view?.notifyDataSetChanged()
        filterDisposable.clear()
        netSearchDisposable.clear()
        networkOffset = 0
        netSearchNow = false
        if (searchNow) {
            filterDisposable.add(
                filter(own.data, filter)
                    .fromIOToMain { filteredData ->
                        onFilteredDataReceived(
                            filteredData
                        )
                    }
            )
            startNetSearch(true)
        } else {
            resolveRefreshing()
        }
    }

    private fun startNetSearch(withDelay: Boolean) {
        val filter = filter

        val in_page = Settings.get().main().isCommunities_in_page_search
        val inc = if (in_page) 1000 else 100
        val searchSingle = if (in_page) communitiesInteractor.getActual(
            accountId,
            userId,
            1000,
            networkOffset, false
        ) else communitiesInteractor.search(
            accountId, filter, null,
            null, null, null, 0, 100, networkOffset
        )

        val single = if (withDelay) {
            delayTaskFlow(1000)
                .andThen(searchSingle)
        } else {
            searchSingle
        }
        netSearchNow = true
        resolveRefreshing()
        netSearchDisposable.add(single.map { filterM(it, filter) }
            .fromIOToMain({ data ->
                onSearchDataReceived(data, inc)
            }) { t -> onSearchError(t) })
    }

    private fun onSearchError(t: Throwable) {
        netSearchNow = false
        resolveRefreshing()
        showError(getCauseIfRuntime(t))
    }

    private fun onSearchDataReceived(communities: Pair<List<Community>, Boolean>, inc: Int) {
        netSearchNow = false
        netSearchEndOfContent = communities.second
        resolveRefreshing()
        if (networkOffset == 0) {
            search.replace(communities.first)
            view?.notifyDataSetChanged()
        } else {
            val sizeBefore = search.size()
            val count = communities.first.size
            search.addAll(communities.first)
            view?.notifySearchDataAdded(
                sizeBefore,
                count
            )
        }
        networkOffset += inc
    }

    private fun onFilteredDataReceived(filteredData: List<Community>) {
        filtered.replace(filteredData)
        view?.notifyDataSetChanged()
    }

    fun fireCommunityClick(community: Community) {
        view?.showCommunityWall(
            accountId,
            community
        )
    }

    fun fireUnsubscribe(community: Community) {
        actualDisposable.add(communitiesInteractor.leave(accountId, community.id)
            .fromIOToMain({ fireRefresh() }) { t -> onSearchError(t) })
    }

    fun fireCommunityLongClick(community: Community): Boolean {
        if ((exist(own, community) || exist(filtered, community)) && userId == accountId) {
            view?.showCommunityMenu(
                community
            )
            return true
        }
        return false
    }

    override fun onDestroyed() {
        actualDisposable.cancel()
        cacheDisposable.cancel()
        filterDisposable.cancel()
        netSearchDisposable.cancel()
        super.onDestroyed()
    }

    fun fireRefresh() {
        if (isSearchNow) {
            netSearchDisposable.clear()
            netSearchNow = false
            networkOffset = 0
            startNetSearch(false)
        } else {
            cacheDisposable.clear()
            cacheLoadingNow = false
            actualDisposable.clear()
            actualLoadingNow = false
            offset = 0
            requestActualData(false)
        }
    }

    fun fireScrollToEnd() {
        if (isSearchNow) {
            if (!netSearchNow && !netSearchEndOfContent) {
                startNetSearch(false)
            }
        } else {
            if (!actualLoadingNow && !cacheLoadingNow && !actualEndOfContent) {
                requestActualData(false)
            }
        }
    }

    companion object {
        internal fun filter(orig: List<Community>, filter: String?): Flow<List<Community>> {
            return flow {
                val result: MutableList<Community> = ArrayList(5)
                for (community in orig) {
                    if (!isActive()) {
                        break
                    }
                    if (isMatchFilter(community, filter)) {
                        result.add(community)
                    }
                }
                emit(result)
            }
        }

        internal fun filterM(
            orig: List<Community>,
            filter: String?
        ): Pair<List<Community>, Boolean> {
            val result: MutableList<Community> = ArrayList(5)
            for (community in orig) {
                if (isMatchFilter(community, filter)) {
                    result.add(community)
                }
            }
            return Pair(result, orig.isEmpty())
        }

        private fun isMatchFilter(community: Community, filter: String?): Boolean {
            if (filter.trimmedIsNullOrEmpty()) {
                return true
            }
            val lower = filter.lowercase(Locale.getDefault()).trim()
            community.fullName.nonNullNoEmpty {
                val lowername = it.lowercase(Locale.getDefault())
                if (lowername.contains(lower)) {
                    return true
                }
                try {
                    val t = cyr2lat(lower)
                    if (t != null && lowername.contains(t)) {
                        return true
                    }
                } catch (_: Exception) {
                }
                try {
                    val t = lat2cyr(lower)
                    //Caused by java.lang.StringIndexOutOfBoundsException: length=3; index=3
                    if (t != null && lowername.contains(t)) {
                        return true
                    }
                } catch (_: Exception) {
                }
            }
            return community.domain.nonNullNoEmpty() && community.domain?.lowercase(Locale.getDefault())
                ?.contains(lower) == true
        }

        internal fun exist(data: DataWrapper<Community>?, community: Community?): Boolean {
            if (data == null || community == null) {
                return false
            }
            for (i in 0 until data.size()) {
                if (data.data[i].ownerId == community.ownerId) {
                    return true
                }
            }
            return false
        }
    }

    init {
        loadCachedData()
        if (!isNotFriendShow) {
            requestActualData(false)
        }
    }
}