package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.model.Document
import kotlinx.coroutines.flow.Flow

interface IDocsInteractor {
    fun request(accountId: Long, ownerId: Long, filter: Int): Flow<List<Document>>
    fun getCacheData(accountId: Long, ownerId: Long, filter: Int): Flow<List<Document>>
    fun add(accountId: Long, docId: Int, ownerId: Long, accessKey: String?): Flow<Int>
    fun findById(accountId: Long, ownerId: Long, docId: Int, accessKey: String?): Flow<Document>
    fun search(
        accountId: Long,
        criteria: DocumentSearchCriteria,
        count: Int,
        offset: Int
    ): Flow<List<Document>>

    fun delete(accountId: Long, docId: Int, ownerId: Long): Flow<Boolean>
}