package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.model.Document
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IDocsInteractor {
    fun request(accountId: Long, ownerId: Long, filter: Int): Single<List<Document>>
    fun getCacheData(accountId: Long, ownerId: Long, filter: Int): Single<List<Document>>
    fun add(accountId: Long, docId: Int, ownerId: Long, accessKey: String?): Single<Int>
    fun findById(accountId: Long, ownerId: Long, docId: Int, accessKey: String?): Single<Document>
    fun search(
        accountId: Long,
        criteria: DocumentSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<Document>>

    fun delete(accountId: Long, docId: Int, ownerId: Long): Completable
}