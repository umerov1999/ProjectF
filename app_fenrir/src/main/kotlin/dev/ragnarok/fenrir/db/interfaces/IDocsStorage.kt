package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity
import dev.ragnarok.fenrir.model.criteria.DocsCriteria
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IDocsStorage : IStorage {
    @CheckResult
    operator fun get(criteria: DocsCriteria): Single<List<DocumentDboEntity>>

    @CheckResult
    fun store(
        accountId: Long,
        ownerId: Long,
        entities: List<DocumentDboEntity>,
        clearBeforeInsert: Boolean
    ): Completable

    @CheckResult
    fun delete(accountId: Long, docId: Int, ownerId: Long): Completable
}