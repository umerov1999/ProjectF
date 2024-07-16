package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Topic
import kotlinx.coroutines.flow.Flow

interface IBoardInteractor {
    fun getCachedTopics(accountId: Long, ownerId: Long): Flow<List<Topic>>
    fun getActualTopics(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Flow<List<Topic>>
}