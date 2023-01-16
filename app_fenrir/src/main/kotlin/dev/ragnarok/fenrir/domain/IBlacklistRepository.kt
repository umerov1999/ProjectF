package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface IBlacklistRepository {
    fun fireAdd(accountId: Long, owner: Owner): Completable
    fun fireRemove(accountId: Long, ownerId: Long): Completable
    fun observeAdding(): Observable<Pair<Long, Owner>>
    fun observeRemoving(): Observable<Pair<Long, Long>>
}