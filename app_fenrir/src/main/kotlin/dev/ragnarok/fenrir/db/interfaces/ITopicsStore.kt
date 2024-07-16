package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PollDboEntity
import dev.ragnarok.fenrir.db.model.entity.TopicDboEntity
import dev.ragnarok.fenrir.model.criteria.TopicsCriteria
import kotlinx.coroutines.flow.Flow

interface ITopicsStore {
    @CheckResult
    fun getByCriteria(criteria: TopicsCriteria): Flow<List<TopicDboEntity>>

    @CheckResult
    fun store(
        accountId: Long,
        ownerId: Long,
        topics: List<TopicDboEntity>,
        owners: OwnerEntities?,
        canAddTopic: Boolean,
        defaultOrder: Int,
        clearBefore: Boolean
    ): Flow<Boolean>

    @CheckResult
    fun attachPoll(
        accountId: Long,
        ownerId: Long,
        topicId: Int,
        pollDbo: PollDboEntity?
    ): Flow<Boolean>
}