package dev.ragnarok.fenrir.domain

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface IAttachmentsRepository {
    @CheckResult
    fun remove(
        accountId: Long,
        @AttachToType type: Int,
        attachToId: Int,
        generatedAttachmentId: Int
    ): Completable

    @CheckResult
    fun attach(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int,
        models: List<AbsModel>
    ): Completable

    fun getAttachmentsWithIds(
        accountId: Long,
        @AttachToType attachToType: Int,
        attachToDbid: Int
    ): Single<List<Pair<Int, AbsModel>>>

    fun observeAdding(): Observable<IAddEvent>
    fun observeRemoving(): Observable<IRemoveEvent>
    interface IBaseEvent {
        val accountId: Long

        @get:AttachToType
        val attachToType: Int
        val attachToId: Int
    }

    interface IRemoveEvent : IBaseEvent {
        val generatedId: Int
    }

    interface IAddEvent : IBaseEvent {
        val attachments: List<Pair<Int, AbsModel>>
    }
}