package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Community
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface ICommunitiesInteractor {
    fun getCachedData(accountId: Long, userId: Long): Single<List<Community>>
    fun getActual(
        accountId: Long,
        userId: Long,
        count: Int,
        offset: Int,
        store: Boolean
    ): Single<List<Community>>

    fun search(
        accountId: Long,
        q: String?,
        type: String?,
        countryId: Int?,
        cityId: Int?,
        futureOnly: Boolean?,
        sort: Int?,
        count: Int,
        offset: Int
    ): Single<List<Community>>

    fun join(accountId: Long, groupId: Long): Completable
    fun leave(accountId: Long, groupId: Long): Completable
}