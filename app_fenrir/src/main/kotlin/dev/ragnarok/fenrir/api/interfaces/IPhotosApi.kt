package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiPhotoAlbum
import dev.ragnarok.fenrir.api.model.VKApiPhotoTags
import dev.ragnarok.fenrir.api.model.VKApiPrivacy
import dev.ragnarok.fenrir.api.model.interfaces.IAttachmentToken
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.UploadChatPhotoResponse
import dev.ragnarok.fenrir.api.model.response.UploadOwnerPhotoResponse
import dev.ragnarok.fenrir.api.model.server.VKApiChatPhotoUploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiOwnerPhotoUploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiPhotoMessageServer
import dev.ragnarok.fenrir.api.model.server.VKApiUploadServer
import dev.ragnarok.fenrir.api.model.server.VKApiWallUploadServer
import kotlinx.coroutines.flow.Flow

interface IPhotosApi {
    @CheckResult
    fun deleteAlbum(albumId: Int, groupId: Long?): Flow<Boolean>

    @CheckResult
    fun restore(ownerId: Long?, photoId: Int): Flow<Boolean>

    @CheckResult
    fun delete(ownerId: Long?, photoId: Int): Flow<Boolean>

    @CheckResult
    fun deleteComment(ownerId: Long?, commentId: Int): Flow<Boolean>

    @CheckResult
    fun restoreComment(ownerId: Long?, commentId: Int): Flow<Boolean>

    @CheckResult
    fun editComment(
        ownerId: Long?, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean>

    @CheckResult
    fun createAlbum(
        title: String?, groupId: Long?, description: String?,
        privacyView: VKApiPrivacy?, privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?, commentsDisabled: Boolean?
    ): Flow<VKApiPhotoAlbum>

    @CheckResult
    fun editAlbum(
        albumId: Int, title: String?, description: String?, ownerId: Long?,
        privacyView: VKApiPrivacy?, privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?, commentsDisabled: Boolean?
    ): Flow<Boolean>

    @CheckResult
    fun copy(ownerId: Long, photoId: Int, accessKey: String?): Flow<Int>

    @CheckResult
    fun createComment(
        ownerId: Long?, photoId: Int, fromGroup: Boolean?, message: String?,
        replyToComment: Int?, attachments: Collection<IAttachmentToken>?,
        stickerId: Int?, accessKey: String?, generatedUniqueId: Int?
    ): Flow<Int>

    @CheckResult
    fun getComments(
        ownerId: Long?, photoId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?, sort: String?,
        accessKey: String?, extended: Boolean?, fields: String?
    ): Flow<DefaultCommentsResponse>

    @CheckResult
    fun getById(ids: Collection<AccessIdPair>): Flow<List<VKApiPhoto>>

    @CheckResult
    fun getUploadServer(albumId: Int, groupId: Long?): Flow<VKApiUploadServer>

    @CheckResult
    fun saveOwnerPhoto(
        server: String?,
        hash: String?,
        photo: String?
    ): Flow<UploadOwnerPhotoResponse>

    @CheckResult
    fun getOwnerPhotoUploadServer(ownerId: Long?): Flow<VKApiOwnerPhotoUploadServer>

    @CheckResult
    fun getChatUploadServer(chat_id: Long?): Flow<VKApiChatPhotoUploadServer>

    @CheckResult
    fun setChatPhoto(file: String?): Flow<UploadChatPhotoResponse>

    @CheckResult
    fun saveWallPhoto(
        userId: Long?, groupId: Long?, photo: String?, server: Long,
        hash: String?, latitude: Double?, longitude: Double?, caption: String?
    ): Flow<List<VKApiPhoto>>

    @CheckResult
    fun getWallUploadServer(groupId: Long?): Flow<VKApiWallUploadServer>

    @CheckResult
    fun save(
        albumId: Int, groupId: Long?, server: Long, photosList: String?, hash: String?,
        latitude: Double?, longitude: Double?, caption: String?
    ): Flow<List<VKApiPhoto>>

    @CheckResult
    operator fun get(
        ownerId: Long?, albumId: String?, photoIds: Collection<Int>?, rev: Boolean?,
        offset: Int?, count: Int?
    ): Flow<Items<VKApiPhoto>>

    @CheckResult
    fun getUsersPhoto(
        ownerId: Long?,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiPhoto>>

    @CheckResult
    fun getAll(
        ownerId: Long?,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiPhoto>>

    @get:CheckResult
    val messagesUploadServer: Flow<VKApiPhotoMessageServer>

    @CheckResult
    fun saveMessagesPhoto(server: Long?, photo: String?, hash: String?): Flow<List<VKApiPhoto>>

    @CheckResult
    fun getAlbums(
        ownerId: Long?, albumIds: Collection<Int>?, offset: Int?,
        count: Int?, needSystem: Boolean?, needCovers: Boolean?
    ): Flow<Items<VKApiPhotoAlbum>>

    @CheckResult
    fun getTags(ownerId: Long?, photo_id: Int?, access_key: String?): Flow<List<VKApiPhotoTags>>

    @CheckResult
    fun getAllComments(
        ownerId: Long?,
        album_id: Int?,
        need_likes: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiComment>>

    @CheckResult
    fun search(
        q: String?,
        lat_gps: Double?,
        long_gps: Double?,
        sort: Int?,
        radius: Int?,
        startTime: Long?,
        endTime: Long?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiPhoto>>
}
