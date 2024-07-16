package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity
import dev.ragnarok.fenrir.model.criteria.DocsCriteria
import kotlinx.coroutines.flow.Flow

interface IDocsStorage : IStorage {
    @CheckResult
    operator fun get(criteria: DocsCriteria): Flow<List<DocumentDboEntity>>

    @CheckResult
    fun store(
        accountId: Long,
        ownerId: Long,
        entities: List<DocumentDboEntity>,
        clearBeforeInsert: Boolean
    ): Flow<Boolean>

    @CheckResult
    fun delete(accountId: Long, docId: Int, ownerId: Long): Flow<Boolean>
}