package dev.ragnarok.fenrir.fragment.search.peoplesearch

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.Repository.owners
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.PeopleSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Single

class PeopleSearchPresenter(
    accountId: Int,
    criteria: PeopleSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IPeopleSearchView, PeopleSearchCriteria, User, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val ownersRepository: IOwnersRepository = owners
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): PeopleSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Int,
        criteria: PeopleSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<User>, IntNextFrom>> {
        val offset = startFrom.offset
        val nextOffset = offset + 50
        return ownersRepository.searchPeoples(accountId, criteria, 50, offset)
            .map { users -> create(users, IntNextFrom(nextOffset)) }
    }

    override fun instantiateEmptyCriteria(): PeopleSearchCriteria {
        return PeopleSearchCriteria("")
    }

    override fun canSearch(criteria: PeopleSearchCriteria?): Boolean {
        return true
    }

    fun fireUserClick(user: User) {
        view?.openUserWall(
            accountId,
            user
        )
    }
}