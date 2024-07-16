package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.feedback.FeedbackEntity
import dev.ragnarok.fenrir.model.FeedbackVKOfficial
import dev.ragnarok.fenrir.model.criteria.NotificationsCriteria
import kotlinx.coroutines.flow.Flow

interface IFeedbackStorage : IStorage {
    fun insert(
        accountId: Long,
        dbos: List<FeedbackEntity>,
        owners: OwnerEntities?,
        clearBefore: Boolean
    ): Flow<IntArray>

    fun findByCriteria(criteria: NotificationsCriteria): Flow<List<FeedbackEntity>>

    fun insertOfficial(
        accountId: Long,
        dbos: List<FeedbackVKOfficial>,
        clearBefore: Boolean
    ): Flow<IntArray>

    fun findByCriteriaOfficial(criteria: NotificationsCriteria): Flow<List<FeedbackVKOfficial>>
}