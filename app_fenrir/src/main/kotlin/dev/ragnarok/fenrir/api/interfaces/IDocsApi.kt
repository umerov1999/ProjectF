package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.api.model.server.VKApiDocsUploadServer
import kotlinx.coroutines.flow.Flow

interface IDocsApi {
    @CheckResult
    fun delete(ownerId: Long?, docId: Int): Flow<Boolean>

    @CheckResult
    fun add(ownerId: Long, docId: Int, accessKey: String?): Flow<Int>

    @CheckResult
    fun getById(pairs: Collection<AccessIdPair>): Flow<List<VKApiDoc>>

    @CheckResult
    fun search(query: String?, count: Int?, offset: Int?): Flow<Items<VKApiDoc>>

    @CheckResult
    fun save(file: String?, title: String?, tags: String?): Flow<VKApiDoc.Entry>

    @CheckResult
    fun getUploadServer(groupId: Long?): Flow<VKApiDocsUploadServer>

    @CheckResult
    fun getMessagesUploadServer(peerId: Long?, type: String?): Flow<VKApiDocsUploadServer>

    @CheckResult
    operator fun get(
        ownerId: Long?,
        count: Int?,
        offset: Int?,
        type: Int?
    ): Flow<Items<VKApiDoc>>
}