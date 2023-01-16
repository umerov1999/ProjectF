package dev.ragnarok.fenrir.domain.impl

import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.db.interfaces.IDocsStorage
import dev.ragnarok.fenrir.db.model.entity.DocumentDboEntity
import dev.ragnarok.fenrir.domain.IDocsInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Entity.mapDoc
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.domain.mappers.Entity2Model.buildDocumentFromDbo
import dev.ragnarok.fenrir.exception.NotFoundException
import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.model.Document
import dev.ragnarok.fenrir.model.criteria.DocsCriteria
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class DocsInteractor(private val networker: INetworker, private val cache: IDocsStorage) :
    IDocsInteractor {
    override fun request(accountId: Long, ownerId: Long, filter: Int): Single<List<Document>> {
        return networker.vkDefault(accountId)
            .docs()[ownerId, null, null, filter]
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .flatMap { dtos ->
                val documents: MutableList<Document> = ArrayList(dtos.size)
                val entities: MutableList<DocumentDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    documents.add(transform(dto))
                    entities.add(mapDoc(dto))
                }
                cache.store(accountId, ownerId, entities, true)
                    .andThen(Single.just<List<Document>>(documents))
            }
    }

    override fun getCacheData(accountId: Long, ownerId: Long, filter: Int): Single<List<Document>> {
        return cache[DocsCriteria(accountId, ownerId).setFilter(filter)]
            .map { entities ->
                val documents: MutableList<Document> = ArrayList(entities.size)
                for (entity in entities) {
                    documents.add(buildDocumentFromDbo(entity))
                }
                documents
            }
    }

    override fun add(accountId: Long, docId: Int, ownerId: Long, accessKey: String?): Single<Int> {
        return networker.vkDefault(accountId)
            .docs()
            .add(ownerId, docId, accessKey)
    }

    override fun findById(
        accountId: Long,
        ownerId: Long,
        docId: Int,
        accessKey: String?
    ): Single<Document> {
        return networker.vkDefault(accountId)
            .docs()
            .getById(listOf(AccessIdPair(docId, ownerId, accessKey)))
            .map { dtos ->
                if (dtos.isEmpty()) {
                    throw NotFoundException()
                }
                transform(dtos[0])
            }
    }

    override fun search(
        accountId: Long,
        criteria: DocumentSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<Document>> {
        return networker.vkDefault(accountId)
            .docs()
            .search(criteria.query, count, offset)
            .map { items ->
                val dtos = listEmptyIfNull(
                    items.items
                )
                val documents: MutableList<Document> = ArrayList()
                for (dto in dtos) {
                    documents.add(transform(dto))
                }
                documents
            }
    }

    override fun delete(accountId: Long, docId: Int, ownerId: Long): Completable {
        return networker.vkDefault(accountId)
            .docs()
            .delete(ownerId, docId)
            .flatMapCompletable { cache.delete(accountId, docId, ownerId) }
    }
}