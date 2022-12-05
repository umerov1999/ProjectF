package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.db.AttachToType
import dev.ragnarok.fenrir.db.interfaces.IAttachmentsStorage
import dev.ragnarok.fenrir.domain.IAttachmentsRepository
import dev.ragnarok.fenrir.domain.IAttachmentsRepository.*
import dev.ragnarok.fenrir.domain.IOwnersRepository
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildAttachmentFromDbo
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.fillOwnerIds
import dev.ragnarok.fenrir.domain.mappers.Model2Entity.buildDboAttachments
import dev.ragnarok.fenrir.model.AbsModel
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.VKOwnIds
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject

class AttachmentsRepository(
    private val store: IAttachmentsStorage,
    private val ownersRepository: IOwnersRepository
) : IAttachmentsRepository {
    private val addPublishSubject: PublishSubject<IAddEvent> = PublishSubject.create()
    private val removePublishSubject: PublishSubject<IRemoveEvent> = PublishSubject.create()
    override fun remove(
        accountId: Int,
        type: Int,
        attachToId: Int,
        generatedAttachmentId: Int
    ): Completable {
        return store.remove(accountId, type, attachToId, generatedAttachmentId)
            .doOnComplete {
                val event =
                    RemoveEvent(accountId, type, attachToId, generatedAttachmentId)
                removePublishSubject.onNext(event)
            }
    }

    override fun attach(
        accountId: Int,
        attachToType: Int,
        attachToDbid: Int,
        models: List<AbsModel>
    ): Completable {
        val entities = buildDboAttachments(models)
        return store.attachDbos(accountId, attachToType, attachToDbid, entities)
            .doAfterSuccess { ids: IntArray ->
                val events: MutableList<Pair<Int, AbsModel>> = ArrayList(models.size)
                for (i in models.indices) {
                    val model = models[i]
                    val generatedId = ids[i]
                    events.add(create(generatedId, model))
                }
                val event = AddEvent(accountId, attachToType, attachToDbid, events)
                addPublishSubject.onNext(event)
            }
            .ignoreElement()
    }

    override fun getAttachmentsWithIds(
        accountId: Int,
        attachToType: Int,
        attachToDbid: Int
    ): Single<List<Pair<Int, AbsModel>>> {
        return store.getAttachmentsDbosWithIds(accountId, attachToType, attachToDbid)
            .flatMap { pairs ->
                val ids = VKOwnIds()
                for (pair in pairs) {
                    fillOwnerIds(ids, pair.second)
                }
                ownersRepository
                    .findBaseOwnersDataAsBundle(accountId, ids.all, IOwnersRepository.MODE_ANY)
                    .map<List<Pair<Int, AbsModel>>> {
                        val models: MutableList<Pair<Int, AbsModel>> = ArrayList(pairs.size)
                        for (pair in pairs) {
                            val model = buildAttachmentFromDbo(pair.second, it)
                            models.add(create(pair.first, model))
                        }
                        models
                    }
            }
    }

    override fun observeAdding(): Observable<IAddEvent> {
        return addPublishSubject
    }

    override fun observeRemoving(): Observable<IRemoveEvent> {
        return removePublishSubject
    }

    private class AddEvent(
        accountId: Int,
        @AttachToType attachToType: Int,
        attachToId: Int,
        override val attachments: List<Pair<Int, AbsModel>>
    ) : Event(accountId, attachToType, attachToId), IAddEvent

    private open class Event(
        override val accountId: Int,
        @AttachToType override val attachToType: Int,
        override val attachToId: Int
    ) : IBaseEvent

    private inner class RemoveEvent(
        accountId: Int,
        @AttachToType attachToType: Int,
        attachToId: Int,
        override val generatedId: Int
    ) : Event(accountId, attachToType, attachToId), IRemoveEvent

}