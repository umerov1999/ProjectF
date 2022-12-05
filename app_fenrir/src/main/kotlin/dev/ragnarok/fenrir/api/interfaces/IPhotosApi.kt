package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.UploadChatPhotoResponse
import dev.ragnarok.fenrir.api.model.response.UploadOwnerPhotoResponse
import dev.ragnarok.fenrir.api.model.server.*
import io.reactivex.rxjava3.core.Single

interface IPhotosApi {
    @CheckResult
    fun deleteAlbum(albumId: Int, groupId: Int?): Single<Boolean>

    @CheckResult
    fun restore(ownerId: Int?, photoId: Int): Single<Boolean>

    @CheckResult
    fun delete(ownerId: Int?, photoId: Int): Single<Boolean>

    @CheckResult
    fun deleteComment(ownerId: Int?, commentId: Int): Single<Boolean>

    @CheckResult
    fun restoreComment(ownerId: Int?, commentId: Int): Single<Boolean>

    @CheckResult
    fun editComment(
        ownerId: Int?, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Single<Boolean>

    @CheckResult
    fun createAlbum(
        title: String?, groupId: Int?, description: String?,
        privacyView: VKApiPrivacy?, privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?, commentsDisabled: Boolean?
    ): Single<VKApiPhotoAlbum>

    @CheckResult
    fun editAlbum(
        albumId: Int, title: String?, description: String?, ownerId: Int?,
        privacyView: VKApiPrivacy?, privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?, commentsDisabled: Boolean?
    ): Single<Boolean>

    @CheckResult
    fun copy(ownerId: Int, photoId: Int, accessKey: String?): Single<Int>

    @CheckResult
    fun createComment(
        ownerId: Int?, photoId: Int, fromGroup: Boolean?, message: String?,
        replyToComment: Int?, attachments: Collection<IAttachmentToken>?,
        stickerId: Int?, accessKey: String?, generatedUniqueId: Int?
    ): Single<Int>

    @CheckResult
    fun getComments(
        ownerId: Int?, photoId: Int, needLikes: Boolean?,
        startCommentId: Int?, offset: Int?, count: Int?, sort: String?,
        accessKey: String?, extended: Boolean?, fields: String?
    ): Single<DefaultCommentsResponse>

    @CheckResult
    fun getById(ids: Collection<AccessIdPair>): Single<List<VKApiPhoto>>

    @CheckResult
    fun getUploadServer(albumId: Int, groupId: Int?): Single<VKApiUploadServer>

    @CheckResult
    fun saveOwnerPhoto(
        server: String?,
        hash: String?,
        photo: String?
    ): Single<UploadOwnerPhotoResponse>

    @CheckResult
    fun getOwnerPhotoUploadServer(ownerId: Int?): Single<VKApiOwnerPhotoUploadServer>

    @CheckResult
    fun getChatUploadServer(chat_id: Int?): Single<VKApiChatPhotoUploadServer>

    @CheckResult
    fun setChatPhoto(file: String?): Single<UploadChatPhotoResponse>

    @CheckResult
    fun saveWallPhoto(
        userId: Int?, groupId: Int?, photo: String?, server: Int,
        hash: String?, latitude: Double?, longitude: Double?, caption: String?
    ): Single<List<VKApiPhoto>>

    @CheckResult
    fun getWallUploadServer(groupId: Int?): Single<VKApiWallUploadServer>

    @CheckResult
    fun save(
        albumId: Int, groupId: Int?, server: Int, photosList: String?, hash: String?,
        latitude: Double?, longitude: Double?, caption: String?
    ): Single<List<VKApiPhoto>>

    @CheckResult
    operator fun get(
        ownerId: Int?, albumId: String?, photoIds: Collection<Int?>?, rev: Boolean?,
        offset: Int?, count: Int?
    ): Single<Items<VKApiPhoto>>

    @CheckResult
    fun getUsersPhoto(
        ownerId: Int?,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiPhoto>>

    @CheckResult
    fun getAll(
        ownerId: Int?,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiPhoto>>

    @get:CheckResult
    val messagesUploadServer: Single<VKApiPhotoMessageServer>

    @CheckResult
    fun saveMessagesPhoto(server: Int?, photo: String?, hash: String?): Single<List<VKApiPhoto>>

    @CheckResult
    fun getAlbums(
        ownerId: Int?, albumIds: Collection<Int?>?, offset: Int?,
        count: Int?, needSystem: Boolean?, needCovers: Boolean?
    ): Single<Items<VKApiPhotoAlbum>>

    @CheckResult
    fun getTags(ownerId: Int?, photo_id: Int?, access_key: String?): Single<List<VKApiPhotoTags>>

    @CheckResult
    fun getAllComments(
        ownerId: Int?,
        album_id: Int?,
        need_likes: Int?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiComment>>

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
    ): Single<Items<VKApiPhoto>>
}