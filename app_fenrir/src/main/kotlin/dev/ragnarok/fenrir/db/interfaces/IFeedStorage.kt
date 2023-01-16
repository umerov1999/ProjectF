package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.FeedListEntity
import dev.ragnarok.fenrir.db.model.entity.NewsDboEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.model.FeedSourceCriteria
import dev.ragnarok.fenrir.model.criteria.FeedCriteria
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IFeedStorage : IStorage {
    fun findByCriteria(criteria: FeedCriteria): Single<List<NewsDboEntity>>
    fun store(
        accountId: Long,
        data: List<NewsDboEntity>,
        owners: OwnerEntities?,
        clearBeforeStore: Boolean
    ): Single<IntArray>

    fun storeLists(accountId: Long, entities: List<FeedListEntity>): Completable
    fun getAllLists(criteria: FeedSourceCriteria): Single<List<FeedListEntity>>
}