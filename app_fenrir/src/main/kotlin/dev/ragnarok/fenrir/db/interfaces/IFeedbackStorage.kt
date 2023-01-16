package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.feedback.FeedbackEntity
import dev.ragnarok.fenrir.model.FeedbackVKOfficial
import dev.ragnarok.fenrir.model.criteria.NotificationsCriteria
import io.reactivex.rxjava3.core.Single

interface IFeedbackStorage : IStorage {
    fun insert(
        accountId: Long,
        dbos: List<FeedbackEntity>,
        owners: OwnerEntities?,
        clearBefore: Boolean
    ): Single<IntArray>

    fun findByCriteria(criteria: NotificationsCriteria): Single<List<FeedbackEntity>>

    fun insertOfficial(
        accountId: Long,
        dbos: List<FeedbackVKOfficial>,
        clearBefore: Boolean
    ): Single<IntArray>

    fun findByCriteriaOfficial(criteria: NotificationsCriteria): Single<List<FeedbackVKOfficial>>
}