package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.NewsDboEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.model.criteria.FeedCriteria
import kotlinx.coroutines.flow.Flow

interface IFeedStorage : IStorage {
    fun findByCriteria(criteria: FeedCriteria): Flow<List<NewsDboEntity>>
    fun store(
        accountId: Long,
        data: List<NewsDboEntity>,
        owners: OwnerEntities?,
        clearBeforeStore: Boolean
    ): Flow<IntArray>
}