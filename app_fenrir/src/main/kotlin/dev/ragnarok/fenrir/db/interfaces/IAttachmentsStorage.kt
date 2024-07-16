package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow

interface IAttachmentsStorage : IStorage {
    fun remove(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        generatedAttachmentId: Int
    ): Flow<Boolean>

    fun attachDbos(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        entities: List<DboEntity>
    ): Flow<IntArray>

    fun getCount(accountId: Long, @AttachToType attachToType: Int, attachToDbid: Int): Flow<Int>
    fun getAttachmentsDbosWithIds(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int
    ): Flow<List<Pair<Int, DboEntity>>>

    suspend fun getAttachmentsDbosSync(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        cancelable: Cancelable
    ): MutableList<DboEntity>
}