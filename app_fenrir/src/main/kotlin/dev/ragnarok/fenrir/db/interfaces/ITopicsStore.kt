package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PollDboEntity
import dev.ragnarok.fenrir.db.model.entity.TopicDboEntity
import dev.ragnarok.fenrir.model.criteria.TopicsCriteria
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface ITopicsStore {
    @CheckResult
    fun getByCriteria(criteria: TopicsCriteria): Single<List<TopicDboEntity>>

    @CheckResult
    fun store(
        accountId: Int,
        ownerId: Int,
        topics: List<TopicDboEntity>,
        owners: OwnerEntities?,
        canAddTopic: Boolean,
        defaultOrder: Int,
        clearBefore: Boolean
    ): Completable

    @CheckResult
    fun attachPoll(accountId: Int, ownerId: Int, topicId: Int, pollDbo: PollDboEntity?): Completable
}