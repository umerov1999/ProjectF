package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.DocumentSearchCriteria
import dev.ragnarok.fenrir.model.Document
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IDocsInteractor {
    fun request(accountId: Int, ownerId: Int, filter: Int): Single<List<Document>>
    fun getCacheData(accountId: Int, ownerId: Int, filter: Int): Single<List<Document>>
    fun add(accountId: Int, docId: Int, ownerId: Int, accessKey: String?): Single<Int>
    fun findById(accountId: Int, ownerId: Int, docId: Int, accessKey: String?): Single<Document>
    fun search(
        accountId: Int,
        criteria: DocumentSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<Document>>

    fun delete(accountId: Int, docId: Int, ownerId: Int): Completable
}