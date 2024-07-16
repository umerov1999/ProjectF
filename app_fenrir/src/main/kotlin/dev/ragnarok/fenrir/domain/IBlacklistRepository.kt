package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IBlacklistRepository {
    fun fireAdd(accountId: Long, owner: Owner): Flow<Boolean>
    fun fireRemove(accountId: Long, ownerId: Long): Flow<Boolean>
    fun observeAdding(): SharedFlow<Pair<Long, Owner>>
    fun observeRemoving(): SharedFlow<Pair<Long, Long>>
}