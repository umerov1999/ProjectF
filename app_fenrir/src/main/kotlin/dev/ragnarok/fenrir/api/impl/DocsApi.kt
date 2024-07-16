package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IDocsApi
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.api.model.server.VKApiDocsUploadServer
import dev.ragnarok.fenrir.api.services.IDocsService
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class DocsApi(accountId: Long, provider: IServiceProvider) : AbsApi(accountId, provider),
    IDocsApi {
    override fun delete(ownerId: Long?, docId: Int): Flow<Boolean> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMapConcat {
                it.delete(ownerId, docId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun add(ownerId: Long, docId: Int, accessKey: String?): Flow<Int> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMapConcat {
                it.add(ownerId, docId, accessKey)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(pairs: Collection<AccessIdPair>): Flow<List<VKApiDoc>> {
        val ids =
            join(pairs, ",") { AccessIdPair.format(it) }
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getById(ids)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(query: String?, count: Int?, offset: Int?): Flow<Items<VKApiDoc>> {
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.search(query, count, offset)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun save(file: String?, title: String?, tags: String?): Flow<VKApiDoc.Entry> {
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.save(file, title, tags)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getMessagesUploadServer(
        peerId: Long?,
        type: String?
    ): Flow<VKApiDocsUploadServer> {
        return provideService(IDocsService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getMessagesUploadServer(peerId, type)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getUploadServer(groupId: Long?): Flow<VKApiDocsUploadServer> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMapConcat {
                it.getUploadServer(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        ownerId: Long?,
        count: Int?,
        offset: Int?,
        type: Int?
    ): Flow<Items<VKApiDoc>> {
        return provideService(IDocsService(), TokenType.USER)
            .flatMapConcat {
                it[ownerId, count, offset, type]
                    .map(extractResponseWithErrorHandling())
            }
    }
}