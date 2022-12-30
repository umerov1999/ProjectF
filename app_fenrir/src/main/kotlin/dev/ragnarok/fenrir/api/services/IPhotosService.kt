package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.UploadChatPhotoResponse
import dev.ragnarok.fenrir.api.model.response.UploadOwnerPhotoResponse
import dev.ragnarok.fenrir.api.model.server.*
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IPhotosService : IServiceRest() {
    //https://vk.com/dev/photos.deleteAlbum
    fun deleteAlbum(
        albumId: Int,
        groupId: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.deleteAlbum", form(
                "album_id" to albumId,
                "group_id" to groupId
            ), baseInt
        )
    }

    //https://vk.com/dev/photos.restore
    fun restore(
        ownerId: Int?,
        photoId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.restore", form(
                "owner_id" to ownerId,
                "photo_id" to photoId
            ), baseInt
        )
    }

    //https://vk.com/dev/photos.delete
    fun delete(
        ownerId: Int?,
        photoId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.delete", form(
                "owner_id" to ownerId,
                "photo_id" to photoId
            ), baseInt
        )
    }

    //https://vk.com/dev/photos.deleteComment
    fun deleteComment(
        ownerId: Int?,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.deleteComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId
            ), baseInt
        )
    }

    //https://vk.com/dev/photos.restoreComment
    fun restoreComment(
        ownerId: Int?,
        commentId: Int
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.restoreComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId
            ), baseInt
        )
    }

    //https://vk.com/dev/photos.getComments
    fun getComments(
        ownerId: Int?,
        photoId: Int,
        needLikes: Int?,
        startCommentId: Int?,
        offset: Int?,
        count: Int?,
        sort: String?,
        accessKey: String?,
        extended: Int?,
        fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>> {
        return rest.request(
            "photos.getComments",
            form(
                "owner_id" to ownerId,
                "photo_id" to photoId,
                "need_likes" to needLikes,
                "start_comment_id" to startCommentId,
                "offset" to offset,
                "count" to count,
                "sort" to sort,
                "access_key" to accessKey,
                "extended" to extended,
                "fields" to fields
            ),
            base(DefaultCommentsResponse.serializer())
        )
    }

    /**
     * Edits a comment on a photo.
     *
     * @param ownerId     ID of the user or community that owns the photo. Current user id is used by default
     * @param commentId   Comment ID.
     * @param message     New text of the comment.
     * @param attachments (Required if message is not set.) List of objects attached to the post, in the following format:
     * {type}{owner_id}_{media_id},{type}{owner_id}_{media_id}
     * {type} — Type of media attachment:
     * photo — photo
     * video — video
     * audio — audio
     * doc — document
     * {owner_id} — Media attachment owner ID.
     * {media_id} — Media attachment ID.
     * Example:
     * photo100172_166443618,photo66748_265827614
     * List of comma-separated words
     * @return 1
     */
    fun editComment(
        ownerId: Int?,
        commentId: Int,
        message: String?,
        attachments: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.editComment", form(
                "owner_id" to ownerId,
                "comment_id" to commentId,
                "message" to message,
                "attachments" to attachments
            ), baseInt
        )
    }

    /**
     * Creates an empty photo album.
     *
     * @param title              Album title.
     * @param groupId            ID of the community in which the album will be created.
     * @param description        Album description.
     * @param privacyView        privacy settings view album, list of comma-separated words
     * @param privacyComment     privacy settings album comments, list of comma-separated words
     * @param uploadByAdminsOnly who can upload pictures to an album (only for the community).
     * 0 - photos can add all users;
     * 1 - Photos can be added only editors and administrators.
     * @param commentsDisabled   the album commenting is disabled (only for the community).
     * 0 - commenting on;
     * 1 - commenting is disabled.
     * @return Returns an instance of photo album
     */
    fun createAlbum(
        title: String?,
        groupId: Int?,
        description: String?,
        privacyView: String?,
        privacyComment: String?,
        uploadByAdminsOnly: Int?,
        commentsDisabled: Int?
    ): Single<BaseResponse<VKApiPhotoAlbum>> {
        return rest.request(
            "photos.createAlbum", form(
                "title" to title,
                "group_id" to groupId,
                "description" to description,
                "privacy_view" to privacyView,
                "privacy_comment" to privacyComment,
                "upload_by_admins_only" to uploadByAdminsOnly,
                "comments_disabled" to commentsDisabled
            ), base(VKApiPhotoAlbum.serializer())
        )
    }

    /**
     * Edits information about a photo album.
     *
     * @param albumId            ID of the photo album to be edited.
     * @param title              New album title.
     * @param description        New album description.
     * @param ownerId            ID of the user or community that owns the album. Current user id is used by default
     * @param privacyView        privacy settings view album, list of comma-separated words
     * @param privacyComment     privacy settings album comments, list of comma-separated words
     * @param uploadByAdminsOnly who can upload pictures to an album (only for the community).
     * 0 - photos can add all users;
     * 1 - Photos can be added only editors and administrators.
     * @param commentsDisabled   the album commenting is disabled (only for the community).
     * 0 - commenting on;
     * 1 - commenting is disabled.
     * @return 1
     */
    fun editAlbum(
        albumId: Int,
        title: String?,
        description: String?,
        ownerId: Int?,
        privacyView: String?,
        privacyComment: String?,
        uploadByAdminsOnly: Int?,
        commentsDisabled: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.editAlbum", form(
                "album_id" to albumId,
                "title" to title,
                "description" to description,
                "owner_id" to ownerId,
                "privacy_view" to privacyView,
                "privacy_comment" to privacyComment,
                "upload_by_admins_only" to uploadByAdminsOnly,
                "comments_disabled" to commentsDisabled
            ), baseInt
        )
    }

    /**
     * Allows to copy a photo to the "Saved photos" album
     *
     * @param ownerId   photo's owner ID
     * @param photoId   photo ID
     * @param accessKey special access key for private photos
     * @return Returns the created photo ID.
     */
    fun copy(
        ownerId: Int,
        photoId: Int,
        accessKey: String?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.copy", form(
                "owner_id" to ownerId,
                "photo_id" to photoId,
                "access_key" to accessKey
            ), baseInt
        )
    }

    fun createComment(
        ownerId: Int?,
        photoId: Int,
        fromGroup: Int?,
        message: String?,
        replyToComment: Int?,
        attachments: String?,
        stickerId: Int?,
        accessKey: String?,
        generatedUniqueId: Int?
    ): Single<BaseResponse<Int>> {
        return rest.request(
            "photos.createComment", form(
                "owner_id" to ownerId,
                "photo_id" to photoId,
                "from_group" to fromGroup,
                "message" to message,
                "reply_to_comment" to replyToComment,
                "attachments" to attachments,
                "sticker_id" to stickerId,
                "access_key" to accessKey,
                "guid" to generatedUniqueId
            ), baseInt
        )
    }

    fun getById(
        photos: String?,
        extended: Int?,
        photo_sizes: Int?
    ): Single<BaseResponse<List<VKApiPhoto>>> {
        return rest.request(
            "photos.getById", form(
                "photos" to photos,
                "extended" to extended,
                "photo_sizes" to photo_sizes
            ), baseList(VKApiPhoto.serializer())
        )
    }

    fun getUploadServer(
        albumId: Int,
        groupId: Int?
    ): Single<BaseResponse<VKApiUploadServer>> {
        return rest.request(
            "photos.getUploadServer", form(
                "album_id" to albumId,
                "group_id" to groupId
            ), base(VKApiUploadServer.serializer())
        )
    }

    fun saveOwnerPhoto(
        server: String?,
        hash: String?,
        photo: String?
    ): Single<BaseResponse<UploadOwnerPhotoResponse>> {
        return rest.request(
            "photos.saveOwnerPhoto",
            form(
                "server" to server,
                "hash" to hash,
                "photo" to photo
            ),
            base(UploadOwnerPhotoResponse.serializer())
        )
    }

    fun setChatPhoto(file: String?): Single<BaseResponse<UploadChatPhotoResponse>> {
        return rest.request(
            "messages.setChatPhoto",
            form("file" to file),
            base(UploadChatPhotoResponse.serializer())
        )
    }

    fun getOwnerPhotoUploadServer(ownerId: Int?): Single<BaseResponse<VKApiOwnerPhotoUploadServer>> {
        return rest.request(
            "photos.getOwnerPhotoUploadServer",
            form("owner_id" to ownerId),
            base(VKApiOwnerPhotoUploadServer.serializer())
        )
    }

    fun getChatUploadServer(chat_id: Int?): Single<BaseResponse<VKApiChatPhotoUploadServer>> {
        return rest.request(
            "photos.getChatUploadServer",
            form("chat_id" to chat_id),
            base(VKApiChatPhotoUploadServer.serializer())
        )
    }

    fun saveWallPhoto(
        userId: Int?,
        groupId: Int?,
        photo: String?,
        server: Int,
        hash: String?,
        latitude: Double?,
        longitude: Double?,
        caption: String?
    ): Single<BaseResponse<List<VKApiPhoto>>> {
        return rest.request(
            "photos.saveWallPhoto", form(
                "user_id" to userId,
                "group_id" to groupId,
                "photo" to photo,
                "server" to server,
                "hash" to hash,
                "latitude" to latitude,
                "longitude" to longitude,
                "caption" to caption
            ), baseList(VKApiPhoto.serializer())
        )
    }

    fun getWallUploadServer(groupId: Int?): Single<BaseResponse<VKApiWallUploadServer>> {
        return rest.request(
            "photos.getWallUploadServer",
            form("group_id" to groupId),
            base(VKApiWallUploadServer.serializer())
        )
    }

    fun save(
        albumId: Int,
        groupId: Int?,
        server: Int,
        photosList: String?,
        hash: String?,
        latitude: Double?,
        longitude: Double?,
        caption: String?
    ): Single<BaseResponse<List<VKApiPhoto>>> {
        return rest.request(
            "photos.save", form(
                "album_id" to albumId,
                "group_id" to groupId,
                "server" to server,
                "photos_list" to photosList,
                "hash" to hash,
                "latitude" to latitude,
                "longitude" to longitude,
                "caption" to caption
            ), baseList(VKApiPhoto.serializer())
        )
    }

    operator fun get(
        ownerId: Int?,
        albumId: String?,
        photoIds: String?,
        rev: Int?,
        extended: Int?,
        photoSizes: Int?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>> {
        return rest.request(
            "photos.get", form(
                "owner_id" to ownerId,
                "album_id" to albumId,
                "photo_ids" to photoIds,
                "rev" to rev,
                "extended" to extended,
                "photo_sizes" to photoSizes,
                "offset" to offset,
                "count" to count
            ), items(VKApiPhoto.serializer())
        )
    }

    fun getUserPhotos(
        ownerId: Int?,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>> {
        return rest.request(
            "photos.getUserPhotos", form(
                "user_id" to ownerId,
                "extended" to extended,
                "sort" to sort,
                "offset" to offset,
                "count" to count
            ), items(VKApiPhoto.serializer())
        )
    }

    fun getAll(
        ownerId: Int?,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?,
        no_service_albums: Int?,
        need_hidden: Int?,
        skip_hidden: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>> {
        return rest.request(
            "photos.getAll", form(
                "owner_id" to ownerId,
                "extended" to extended,
                "photo_sizes" to photo_sizes,
                "offset" to offset,
                "count" to count,
                "no_service_albums" to no_service_albums,
                "need_hidden" to need_hidden,
                "skip_hidden" to skip_hidden
            ), items(VKApiPhoto.serializer())
        )
    }

    val messagesUploadServer: Single<BaseResponse<VKApiPhotoMessageServer>>
        get() = rest.request(
            "photos.getMessagesUploadServer",
            null,
            base(VKApiPhotoMessageServer.serializer())
        )

    fun saveMessagesPhoto(
        server: Int?,
        photo: String?,
        hash: String?
    ): Single<BaseResponse<List<VKApiPhoto>>> {
        return rest.request(
            "photos.saveMessagesPhoto", form(
                "server" to server,
                "photo" to photo,
                "hash" to hash
            ), baseList(VKApiPhoto.serializer())
        )
    }

    fun getAlbums(
        ownerId: Int?,
        albumIds: String?,
        offset: Int?,
        count: Int?,
        needSystem: Int?,
        needCovers: Int?,
        photoSizes: Int?
    ): Single<BaseResponse<Items<VKApiPhotoAlbum>>> {
        return rest.request(
            "photos.getAlbums", form(
                "owner_id" to ownerId,
                "album_ids" to albumIds,
                "offset" to offset,
                "count" to count,
                "need_system" to needSystem,
                "need_covers" to needCovers,
                "photo_sizes" to photoSizes
            ), items(VKApiPhotoAlbum.serializer())
        )
    }

    fun getTags(
        ownerId: Int?,
        photo_id: Int?,
        access_key: String?
    ): Single<BaseResponse<List<VKApiPhotoTags>>> {
        return rest.request(
            "photos.getTags", form(
                "owner_id" to ownerId,
                "photo_id" to photo_id,
                "access_key" to access_key
            ), baseList(VKApiPhotoTags.serializer())
        )
    }

    fun getAllComments(
        ownerId: Int?,
        album_id: Int?,
        need_likes: Int?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiComment>>> {
        return rest.request(
            "photos.getAllComments", form(
                "owner_id" to ownerId,
                "album_id" to album_id,
                "need_likes" to need_likes,
                "offset" to offset,
                "count" to count
            ), items(VKApiComment.serializer())
        )
    }

    fun search(
        q: String?,
        lat_gps: Double?,
        long_gps: Double?,
        sort: Int?,
        radius: Int?,
        start_time: Long?,
        end_time: Long?,
        offset: Int?,
        count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>> {
        return rest.request(
            "photos.search", form(
                "q" to q,
                "lat" to lat_gps,
                "long" to long_gps,
                "sort" to sort,
                "radius" to radius,
                "start_time" to start_time,
                "end_time" to end_time,
                "offset" to offset,
                "count" to count
            ), items(VKApiPhoto.serializer())
        )
    }
}