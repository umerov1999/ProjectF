package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Community
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface ICommunitiesInteractor {
    fun getCachedData(accountId: Int, userId: Int): Single<List<Community>>
    fun getActual(
        accountId: Int,
        userId: Int,
        count: Int,
        offset: Int,
        store: Boolean
    ): Single<List<Community>>

    fun search(
        accountId: Int,
        q: String?,
        type: String?,
        countryId: Int?,
        cityId: Int?,
        futureOnly: Boolean?,
        sort: Int?,
        count: Int,
        offset: Int
    ): Single<List<Community>>

    fun join(accountId: Int, groupId: Int): Completable
    fun leave(accountId: Int, groupId: Int): Completable
}