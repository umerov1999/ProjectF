package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.CommentEntity
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.model.CommentUpdate
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.DraftComment
import dev.ragnarok.fenrir.model.criteria.CommentsCriteria
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface ICommentsStorage : IStorage {
    fun insert(
        accountId: Long,
        sourceId: Int,
        sourceOwnerId: Long,
        sourceType: Int,
        dbos: List<CommentEntity>,
        owners: OwnerEntities?,
        clearBefore: Boolean
    ): Single<IntArray>

    fun getDbosByCriteria(criteria: CommentsCriteria): Single<List<CommentEntity>>

    @CheckResult
    fun findEditingComment(accountId: Long, commented: Commented): Maybe<DraftComment>?

    @CheckResult
    fun saveDraftComment(
        accountId: Long,
        commented: Commented,
        text: String?,
        replyToUser: Long,
        replyToComment: Int
    ): Single<Int>

    fun commitMinorUpdate(update: CommentUpdate): Completable
    fun observeMinorUpdates(): Observable<CommentUpdate>
    fun deleteByDbid(accountId: Long, dbid: Int): Completable
}