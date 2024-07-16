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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class DocsInteractor(private val networker: INetworker, private val cache: IDocsStorage) :
    IDocsInteractor {
    override fun request(accountId: Long, ownerId: Long, filter: Int): Flow<List<Document>> {
        return networker.vkDefault(accountId)
            .docs()[ownerId, null, null, filter]
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .flatMapConcat { dtos ->
                val documents: MutableList<Document> = ArrayList(dtos.size)
                val entities: MutableList<DocumentDboEntity> = ArrayList(dtos.size)
                for (dto in dtos) {
                    documents.add(transform(dto))
                    entities.add(mapDoc(dto))
                }
                cache.store(accountId, ownerId, entities, true)
                    .map {
                        documents
                    }
            }
    }

    override fun getCacheData(accountId: Long, ownerId: Long, filter: Int): Flow<List<Document>> {
        return cache[DocsCriteria(accountId, ownerId).setFilter(filter)]
            .map { entities ->
                val documents: MutableList<Document> = ArrayList(entities.size)
                for (entity in entities) {
                    documents.add(buildDocumentFromDbo(entity))
                }
                documents
            }
    }

    override fun add(accountId: Long, docId: Int, ownerId: Long, accessKey: String?): Flow<Int> {
        return networker.vkDefault(accountId)
            .docs()
            .add(ownerId, docId, accessKey)
    }

    override fun findById(
        accountId: Long,
        ownerId: Long,
        docId: Int,
        accessKey: String?
    ): Flow<Document> {
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
    ): Flow<List<Document>> {
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

    override fun delete(accountId: Long, docId: Int, ownerId: Long): Flow<Boolean> {
        return networker.vkDefault(accountId)
            .docs()
            .delete(ownerId, docId)
            .flatMapConcat { cache.delete(accountId, docId, ownerId) }
    }
}