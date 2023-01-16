package dev.ragnarok.fenrir.fragment.search.communitiessearch

import android.os.Bundle
import dev.ragnarok.fenrir.domain.ICommunitiesInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.GroupSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Community
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Single

class CommunitiesSearchPresenter(
    accountId: Long,
    criteria: GroupSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<ICommunitiesSearchView, GroupSearchCriteria, Community, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val communitiesInteractor: ICommunitiesInteractor =
        InteractorFactory.createCommunitiesInteractor()
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): GroupSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Long,
        criteria: GroupSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Community>, IntNextFrom>> {
        val type = extractTypeFromCriteria(criteria)
        val countryId = criteria.extractDatabaseEntryValueId(GroupSearchCriteria.KEY_COUNTRY)
        val cityId = criteria.extractDatabaseEntryValueId(GroupSearchCriteria.KEY_CITY)
        val future = criteria.extractBoleanValueFromOption(GroupSearchCriteria.KEY_FUTURE_ONLY)
        val sortOption: SpinnerOption? = criteria.findOptionByKey(GroupSearchCriteria.KEY_SORT)
        val sort = sortOption?.value?.id
        val offset = startFrom.offset
        val nextFrom = IntNextFrom(offset + 50)
        return communitiesInteractor.search(
            accountId,
            criteria.query,
            type,
            countryId,
            cityId,
            future,
            sort,
            50,
            offset
        )
            .map { communities -> create(communities, nextFrom) }
    }

    override fun instantiateEmptyCriteria(): GroupSearchCriteria {
        return GroupSearchCriteria("")
    }

    override fun canSearch(criteria: GroupSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    fun fireCommunityClick(community: Community) {
        view?.openCommunityWall(
            accountId,
            community
        )
    }

    companion object {
        internal fun extractTypeFromCriteria(criteria: GroupSearchCriteria): String? {
            val option: SpinnerOption? = criteria.findOptionByKey(GroupSearchCriteria.KEY_TYPE)
            if (option?.value != null) {
                when ((option.value ?: return null).id) {
                    GroupSearchCriteria.TYPE_PAGE -> return "page"
                    GroupSearchCriteria.TYPE_GROUP -> return "group"
                    GroupSearchCriteria.TYPE_EVENT -> return "event"
                }
            }
            return null
        }
    }

}