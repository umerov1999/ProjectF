package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.domain.IBlacklistRepository
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.createPublishSubject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow

class BlacklistRepository : IBlacklistRepository {
    private val addPublisher = createPublishSubject<Pair<Long, Owner>>()
    private val removePublisher = createPublishSubject<Pair<Long, Long>>()

    override fun fireAdd(accountId: Long, owner: Owner): Flow<Boolean> {
        return flow {
            addPublisher.emit(create(accountId, owner))
            emit(true)
        }
    }

    override fun fireRemove(accountId: Long, ownerId: Long): Flow<Boolean> {
        return flow {
            removePublisher.emit(create(accountId, ownerId))
            emit(true)
        }
    }

    override fun observeAdding(): SharedFlow<Pair<Long, Owner>> {
        return addPublisher
    }

    override fun observeRemoving(): SharedFlow<Pair<Long, Long>> {
        return removePublisher
    }
}
