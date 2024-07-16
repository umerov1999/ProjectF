package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Community
import kotlinx.coroutines.flow.Flow

interface ICommunitiesInteractor {
    fun getCachedData(accountId: Long, userId: Long): Flow<List<Community>>
    fun getActual(
        accountId: Long,
        userId: Long,
        count: Int,
        offset: Int,
        store: Boolean
    ): Flow<List<Community>>

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
    ): Flow<List<Community>>

    fun join(accountId: Long, groupId: Long): Flow<Boolean>
    fun leave(accountId: Long, groupId: Long): Flow<Boolean>
}