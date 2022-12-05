package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IAttachmentsStorage : IStorage {
    fun remove(
        accountId: Int,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        generatedAttachmentId: Int
    ): Completable

    fun attachDbos(
        accountId: Int,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        entities: List<DboEntity>
    ): Single<IntArray>

    fun getCount(accountId: Int, @AttachToType attachToType: Int, attachToDbid: Int): Single<Int>
    fun getAttachmentsDbosWithIds(
        accountId: Int,
        @AttachToType attachToType: Int,
        attachToDbid: Int
    ): Single<List<Pair<Int, DboEntity>>>

    fun getAttachmentsDbosSync(
        accountId: Int,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        cancelable: Cancelable
    ): MutableList<DboEntity>
}