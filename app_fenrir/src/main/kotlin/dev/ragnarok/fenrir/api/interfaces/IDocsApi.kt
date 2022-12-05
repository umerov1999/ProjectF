package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.api.model.server.VKApiDocsUploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiVideosUploadServer
import io.reactivex.rxjava3.core.Single

interface IDocsApi {
    @CheckResult
    fun delete(ownerId: Int?, docId: Int): Single<Boolean>

    @CheckResult
    fun add(ownerId: Int, docId: Int, accessKey: String?): Single<Int>

    @CheckResult
    fun getById(pairs: Collection<AccessIdPair>): Single<List<VKApiDoc>>

    @CheckResult
    fun search(query: String?, count: Int?, offset: Int?): Single<Items<VKApiDoc>>

    @CheckResult
    fun save(file: String?, title: String?, tags: String?): Single<VKApiDoc.Entry>

    @CheckResult
    fun getUploadServer(groupId: Int?): Single<VKApiDocsUploadServer>

    @CheckResult
    fun getMessagesUploadServer(peerId: Int?, type: String?): Single<VKApiDocsUploadServer>

    @CheckResult
    fun getVideoServer(
        isPrivate: Int?,
        group_id: Int?,
        name: String?
    ): Single<VKApiVideosUploadServer>

    @CheckResult
    operator fun get(
        ownerId: Int?,
        count: Int?,
        offset: Int?,
        type: Int?
    ): Single<Items<VKApiDoc>>
}