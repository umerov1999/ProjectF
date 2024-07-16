package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.TokenType
import dev.ragnarok.fenrir.api.interfaces.IPhotosApi
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
import dev.ragnarok.fenrir.api.services.IPhotosService
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.checkInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

internal class PhotosApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IPhotosApi {
    override fun deleteAlbum(albumId: Int, groupId: Long?): Flow<Boolean> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.deleteAlbum(albumId, groupId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun restore(ownerId: Long?, photoId: Int): Flow<Boolean> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.restore(ownerId, photoId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun delete(ownerId: Long?, photoId: Int): Flow<Boolean> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.delete(ownerId, photoId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun deleteComment(ownerId: Long?, commentId: Int): Flow<Boolean> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.deleteComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun restoreComment(ownerId: Long?, commentId: Int): Flow<Boolean> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.restoreComment(ownerId, commentId)
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun editComment(
        ownerId: Long?, commentId: Int, message: String?,
        attachments: Collection<IAttachmentToken>?
    ): Flow<Boolean> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat { s ->
                s.editComment(
                    ownerId,
                    commentId,
                    message,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) })
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun createAlbum(
        title: String?,
        groupId: Long?,
        description: String?,
        privacyView: VKApiPrivacy?,
        privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?,
        commentsDisabled: Boolean?
    ): Flow<VKApiPhotoAlbum> {
        val privacyViewTxt = privacyView?.buildJsonArray()
        val privacyCommentTxt = privacyComment?.buildJsonArray()
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.createAlbum(
                    title, groupId, description, privacyViewTxt, privacyCommentTxt,
                    integerFromBoolean(uploadByAdminsOnly), integerFromBoolean(commentsDisabled)
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun editAlbum(
        albumId: Int,
        title: String?,
        description: String?,
        ownerId: Long?,
        privacyView: VKApiPrivacy?,
        privacyComment: VKApiPrivacy?,
        uploadByAdminsOnly: Boolean?,
        commentsDisabled: Boolean?
    ): Flow<Boolean> {
        val privacyViewTxt = privacyView?.buildJsonArray()
        val privacyCommentTxt = privacyComment?.buildJsonArray()
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.editAlbum(
                    albumId, title, description, ownerId, privacyViewTxt, privacyCommentTxt,
                    integerFromBoolean(uploadByAdminsOnly), integerFromBoolean(commentsDisabled)
                )
                    .map(extractResponseWithErrorHandling())
                    .checkInt()
            }
    }

    override fun copy(ownerId: Long, photoId: Int, accessKey: String?): Flow<Int> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.copy(ownerId, photoId, accessKey)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun createComment(
        ownerId: Long?, photoId: Int, fromGroup: Boolean?, message: String?,
        replyToComment: Int?, attachments: Collection<IAttachmentToken>?,
        stickerId: Int?, accessKey: String?, generatedUniqueId: Int?
    ): Flow<Int> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat { s ->
                s.createComment(
                    ownerId,
                    photoId,
                    integerFromBoolean(fromGroup),
                    message,
                    replyToComment,
                    join(
                        attachments,
                        ","
                    ) { formatAttachmentToken(it) },
                    stickerId,
                    accessKey,
                    generatedUniqueId
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getComments(
        ownerId: Long?,
        photoId: Int,
        needLikes: Boolean?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        sort: String?,
        accessKey: String?,
        extended: Boolean?,
        fields: String?
    ): Flow<DefaultCommentsResponse> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getComments(
                    ownerId, photoId, integerFromBoolean(needLikes), startCommentId,
                    offset, count, sort, accessKey, integerFromBoolean(extended), fields
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getById(ids: Collection<AccessIdPair>): Flow<List<VKApiPhoto>> {
        val line = join(
            ids,
            ","
        ) { pair -> pair.ownerId.toString() + "_" + pair.id + if (pair.accessKey == null) "" else "_" + pair.accessKey }
        return provideService(IPhotosService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat { s ->
                s.getById(line, 1, 1)
                    .map(extractResponseWithErrorHandling())
                    .map {
                        // пересохраняем access_key, потому что не получим в ответе
                        for (photo in it) {
                            if (photo.access_key == null) {
                                photo.access_key = findAccessKey(ids, photo.id, photo.owner_id)
                            }
                        }
                        listEmptyIfNull(it)
                    }
            }
    }

    override fun getUploadServer(albumId: Int, groupId: Long?): Flow<VKApiUploadServer> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getUploadServer(albumId, groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun saveOwnerPhoto(
        server: String?,
        hash: String?,
        photo: String?
    ): Flow<UploadOwnerPhotoResponse> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.saveOwnerPhoto(server, hash, photo)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getOwnerPhotoUploadServer(ownerId: Long?): Flow<VKApiOwnerPhotoUploadServer> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getOwnerPhotoUploadServer(ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getChatUploadServer(chat_id: Long?): Flow<VKApiChatPhotoUploadServer> {
        return provideService(IPhotosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.getChatUploadServer(chat_id)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun setChatPhoto(file: String?): Flow<UploadChatPhotoResponse> {
        return provideService(IPhotosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.setChatPhoto(file)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun saveWallPhoto(
        userId: Long?, groupId: Long?, photo: String?,
        server: Long, hash: String?, latitude: Double?,
        longitude: Double?, caption: String?
    ): Flow<List<VKApiPhoto>> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.saveWallPhoto(
                    userId,
                    groupId,
                    photo,
                    server,
                    hash,
                    latitude,
                    longitude,
                    caption
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getWallUploadServer(groupId: Long?): Flow<VKApiWallUploadServer> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getWallUploadServer(groupId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun save(
        albumId: Int, groupId: Long?, server: Long, photosList: String?,
        hash: String?, latitude: Double?, longitude: Double?, caption: String?
    ): Flow<List<VKApiPhoto>> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.save(albumId, groupId, server, photosList, hash, latitude, longitude, caption)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        ownerId: Long?, albumId: String?, photoIds: Collection<Int>?,
        rev: Boolean?, offset: Int?, count: Int?
    ): Flow<Items<VKApiPhoto>> {
        val photos = join(photoIds, ",")
        return provideService(IPhotosService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it[ownerId, albumId, photos, integerFromBoolean(rev), 1, 1, offset, count]
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getUsersPhoto(
        ownerId: Long?,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiPhoto>> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getUserPhotos(ownerId, extended, sort, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAll(
        ownerId: Long?,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiPhoto>> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getAll(ownerId, extended, photo_sizes, offset, count, 0, 1, 0)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override val messagesUploadServer: Flow<VKApiPhotoMessageServer>
        get() = provideService(IPhotosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.messagesUploadServer
                    .map(extractResponseWithErrorHandling())
            }

    override fun saveMessagesPhoto(
        server: Long?,
        photo: String?,
        hash: String?
    ): Flow<List<VKApiPhoto>> {
        return provideService(IPhotosService(), TokenType.USER, TokenType.COMMUNITY)
            .flatMapConcat {
                it.saveMessagesPhoto(server, photo, hash)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAlbums(
        ownerId: Long?, albumIds: Collection<Int>?,
        offset: Int?, count: Int?, needSystem: Boolean?,
        needCovers: Boolean?
    ): Flow<Items<VKApiPhotoAlbum>> {
        val ids = join(albumIds, ",")
        return provideService(IPhotosService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.getAlbums(
                    ownerId,
                    ids,
                    offset,
                    count,
                    integerFromBoolean(needSystem),
                    integerFromBoolean(needCovers),
                    1
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getTags(
        ownerId: Long?,
        photo_id: Int?,
        access_key: String?
    ): Flow<List<VKApiPhotoTags>> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getTags(ownerId, photo_id, access_key).map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    override fun getAllComments(
        ownerId: Long?,
        album_id: Int?,
        need_likes: Int?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiComment>> {
        return provideService(IPhotosService(), TokenType.USER)
            .flatMapConcat {
                it.getAllComments(ownerId, album_id, need_likes, offset, count).map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    override fun search(
        q: String?,
        lat_gps: Double?,
        long_gps: Double?,
        sort: Int?,
        radius: Int?,
        startTime: Long?,
        endTime: Long?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiPhoto>> {
        return provideService(IPhotosService(), TokenType.USER, TokenType.SERVICE)
            .flatMapConcat {
                it.search(
                    q,
                    lat_gps,
                    long_gps,
                    sort,
                    radius,
                    startTime,
                    endTime,
                    offset,
                    count
                ).map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    companion object {
        internal fun findAccessKey(
            data: Collection<AccessIdPair>,
            id: Int,
            ownerId: Long
        ): String? {
            for (pair in data) {
                if (pair.id == id && pair.ownerId == ownerId) {
                    return pair.accessKey
                }
            }
            return null
        }
    }
}
