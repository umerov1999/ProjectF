package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IDocsApi
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.api.model.server.VKApiDocsUploadServer
import dev.ragnarok.fenrir.api.services.IDocsService
import io.reactivex.rxjava3.core.Single

internal class DocsApi(accountId: Long, provider: IServiceProvider) : AbsApi(accountId, provider),
    IDocsApi {
    override fun delete(ownerId: Long?, docId: Int): Single<Boolean> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMap { service ->
                service.delete(ownerId, docId)
                    .map(extractResponseWithErrorHandling())
                    .map { it == 1 }
            }
    }

    override fun add(ownerId: Long, docId: Int, accessKey: String?): Single<Int> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMap { service ->
                service.add(ownerId, docId, accessKey)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(pairs: Collection<AccessIdPair>): Single<List<VKApiDoc>> {
        val ids =
            join(pairs, ",") { AccessIdPair.format(it) }
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getById(ids)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(query: String?, count: Int?, offset: Int?): Single<Items<VKApiDoc>> {
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.search(query, count, offset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun save(file: String?, title: String?, tags: String?): Single<VKApiDoc.Entry> {
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.save(file, title, tags)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMessagesUploadServer(
        peerId: Long?,
        type: String?
    ): Single<VKApiDocsUploadServer> {
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMap { service ->
                service.getMessagesUploadServer(peerId, type)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getUploadServer(groupId: Long?): Single<VKApiDocsUploadServer> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMap { service ->
                service.getUploadServer(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        ownerId: Long?,
        count: Int?,
        offset: Int?,
        type: Int?
    ): Single<Items<VKApiDoc>> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMap { service ->
                service[ownerId, count, offset, type]
                    .map(extractResponseWithErrorHandling())
            }
    }
}