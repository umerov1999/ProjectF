package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface IBlacklistRepository {
    fun fireAdd(accountId: Int, owner: Owner): Completable
    fun fireRemove(accountId: Int, ownerId: Int): Completable
    fun observeAdding(): Observable<Pair<Int, Owner>>
    fun observeRemoving(): Observable<Pair<Int, Int>>
}