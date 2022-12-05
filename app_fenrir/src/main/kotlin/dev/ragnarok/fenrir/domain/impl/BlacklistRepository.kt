package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.domain.IBlacklistRepository
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

class BlacklistRepository : IBlacklistRepository {
    private val addPublisher: PublishSubject<Pair<Int, Owner>> = PublishSubject.create()
    private val removePublisher: PublishSubject<Pair<Int, Int>> = PublishSubject.create()

    override fun fireAdd(accountId: Int, owner: Owner): Completable {
        return Completable.fromAction { addPublisher.onNext(create(accountId, owner)) }
    }

    override fun fireRemove(accountId: Int, ownerId: Int): Completable {
        return Completable.fromAction { removePublisher.onNext(create(accountId, ownerId)) }
    }

    override fun observeAdding(): Observable<Pair<Int, Owner>> {
        return addPublisher
    }

    override fun observeRemoving(): Observable<Pair<Int, Int>> {
        return removePublisher
    }

}