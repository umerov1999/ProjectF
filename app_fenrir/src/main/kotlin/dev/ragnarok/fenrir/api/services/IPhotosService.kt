package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.UploadChatPhotoResponse
import dev.ragnarok.fenrir.api.model.response.UploadOwnerPhotoResponse
import dev.ragnarok.fenrir.api.model.server.*
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface IPhotosService {
    //https://vk.com/dev/photos.deleteAlbum
    @FormUrlEncoded
    @POST("photos.deleteAlbum")
    fun deleteAlbum(
        @Field("album_id") albumId: Int,
        @Field("group_id") groupId: Int?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/photos.restore
    @FormUrlEncoded
    @POST("photos.restore")
    fun restore(
        @Field("owner_id") ownerId: Int?,
        @Field("photo_id") photoId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/photos.delete
    @FormUrlEncoded
    @POST("photos.delete")
    fun delete(
        @Field("owner_id") ownerId: Int?,
        @Field("photo_id") photoId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/photos.deleteComment
    @FormUrlEncoded
    @POST("photos.deleteComment")
    fun deleteComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/photos.restoreComment
    @FormUrlEncoded
    @POST("photos.restoreComment")
    fun restoreComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/photos.getComments
    @FormUrlEncoded
    @POST("photos.getComments")
    fun getComments(
        @Field("owner_id") ownerId: Int?,
        @Field("photo_id") photoId: Int,
        @Field("need_likes") needLikes: Int?,
        @Field("start_comment_id") startCommentId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("sort") sort: String?,
        @Field("access_key") accessKey: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>>

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
    @FormUrlEncoded
    @POST("photos.editComment")
    fun editComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?
    ): Single<BaseResponse<Int>>

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
    @FormUrlEncoded
    @POST("photos.createAlbum")
    fun createAlbum(
        @Field("title") title: String?,
        @Field("group_id") groupId: Int?,
        @Field("description") description: String?,
        @Field("privacy_view") privacyView: String?,
        @Field("privacy_comment") privacyComment: String?,
        @Field("upload_by_admins_only") uploadByAdminsOnly: Int?,
        @Field("comments_disabled") commentsDisabled: Int?
    ): Single<BaseResponse<VKApiPhotoAlbum>>

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
    @FormUrlEncoded
    @POST("photos.editAlbum")
    fun editAlbum(
        @Field("album_id") albumId: Int,
        @Field("title") title: String?,
        @Field("description") description: String?,
        @Field("owner_id") ownerId: Int?,
        @Field("privacy_view") privacyView: String?,
        @Field("privacy_comment") privacyComment: String?,
        @Field("upload_by_admins_only") uploadByAdminsOnly: Int?,
        @Field("comments_disabled") commentsDisabled: Int?
    ): Single<BaseResponse<Int>>

    /**
     * Allows to copy a photo to the "Saved photos" album
     *
     * @param ownerId   photo's owner ID
     * @param photoId   photo ID
     * @param accessKey special access key for private photos
     * @return Returns the created photo ID.
     */
    @FormUrlEncoded
    @POST("photos.copy")
    fun copy(
        @Field("owner_id") ownerId: Int,
        @Field("photo_id") photoId: Int,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("photos.createComment")
    fun createComment(
        @Field("owner_id") ownerId: Int?,
        @Field("photo_id") photoId: Int,
        @Field("from_group") fromGroup: Int?,
        @Field("message") message: String?,
        @Field("reply_to_comment") replyToComment: Int?,
        @Field("attachments") attachments: String?,
        @Field("sticker_id") stickerId: Int?,
        @Field("access_key") accessKey: String?,
        @Field("guid") generatedUniqueId: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("photos.getById")
    fun getById(
        @Field("photos") photos: String?,
        @Field("extended") extended: Int?,
        @Field("photo_sizes") photo_sizes: Int?
    ): Single<BaseResponse<List<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("photos.getUploadServer")
    fun getUploadServer(
        @Field("album_id") albumId: Int,
        @Field("group_id") groupId: Int?
    ): Single<BaseResponse<VKApiUploadServer>>

    @FormUrlEncoded
    @POST("photos.saveOwnerPhoto")
    fun saveOwnerPhoto(
        @Field("server") server: String?,
        @Field("hash") hash: String?,
        @Field("photo") photo: String?
    ): Single<BaseResponse<UploadOwnerPhotoResponse>>

    @FormUrlEncoded
    @POST("messages.setChatPhoto")
    fun setChatPhoto(@Field("file") file: String?): Single<BaseResponse<UploadChatPhotoResponse>>

    @FormUrlEncoded
    @POST("photos.getOwnerPhotoUploadServer")
    fun getOwnerPhotoUploadServer(@Field("owner_id") ownerId: Int?): Single<BaseResponse<VKApiOwnerPhotoUploadServer>>

    @FormUrlEncoded
    @POST("photos.getChatUploadServer")
    fun getChatUploadServer(@Field("chat_id") chat_id: Int?): Single<BaseResponse<VKApiChatPhotoUploadServer>>

    @FormUrlEncoded
    @POST("photos.saveWallPhoto")
    fun saveWallPhoto(
        @Field("user_id") userId: Int?,
        @Field("group_id") groupId: Int?,
        @Field("photo") photo: String?,
        @Field("server") server: Int,
        @Field("hash") hash: String?,
        @Field("latitude") latitude: Double?,
        @Field("longitude") longitude: Double?,
        @Field("caption") caption: String?
    ): Single<BaseResponse<List<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("photos.getWallUploadServer")
    fun getWallUploadServer(@Field("group_id") groupId: Int?): Single<BaseResponse<VKApiWallUploadServer>>

    @FormUrlEncoded
    @POST("photos.save")
    fun save(
        @Field("album_id") albumId: Int,
        @Field("group_id") groupId: Int?,
        @Field("server") server: Int,
        @Field("photos_list") photosList: String?,
        @Field("hash") hash: String?,
        @Field("latitude") latitude: Double?,
        @Field("longitude") longitude: Double?,
        @Field("caption") caption: String?
    ): Single<BaseResponse<List<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("photos.get")
    operator fun get(
        @Field("owner_id") ownerId: Int?,
        @Field("album_id") albumId: String?,
        @Field("photo_ids") photoIds: String?,
        @Field("rev") rev: Int?,
        @Field("extended") extended: Int?,
        @Field("photo_sizes") photoSizes: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("photos.getUserPhotos")
    fun getUserPhotos(
        @Field("user_id") ownerId: Int?,
        @Field("extended") extended: Int?,
        @Field("sort") sort: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("photos.getAll")
    fun getAll(
        @Field("owner_id") ownerId: Int?,
        @Field("extended") extended: Int?,
        @Field("photo_sizes") photo_sizes: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("no_service_albums") no_service_albums: Int?,
        @Field("need_hidden") need_hidden: Int?,
        @Field("skip_hidden") skip_hidden: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>>

    @get:GET("photos.getMessagesUploadServer")
    val messagesUploadServer: Single<BaseResponse<VKApiPhotoMessageServer>>

    @FormUrlEncoded
    @POST("photos.saveMessagesPhoto")
    fun saveMessagesPhoto(
        @Field("server") server: Int?,
        @Field("photo") photo: String?,
        @Field("hash") hash: String?
    ): Single<BaseResponse<List<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("photos.getAlbums")
    fun getAlbums(
        @Field("owner_id") ownerId: Int?,
        @Field("album_ids") albumIds: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("need_system") needSystem: Int?,
        @Field("need_covers") needCovers: Int?,
        @Field("photo_sizes") photoSizes: Int?
    ): Single<BaseResponse<Items<VKApiPhotoAlbum>>>

    @FormUrlEncoded
    @POST("photos.getTags")
    fun getTags(
        @Field("owner_id") ownerId: Int?,
        @Field("photo_id") photo_id: Int?,
        @Field("access_key") access_key: String?
    ): Single<BaseResponse<List<VKApiPhotoTags>>>

    @FormUrlEncoded
    @POST("photos.getAllComments")
    fun getAllComments(
        @Field("owner_id") ownerId: Int?,
        @Field("album_id") album_id: Int?,
        @Field("need_likes") need_likes: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiComment>>>

    @FormUrlEncoded
    @POST("photos.search")
    fun search(
        @Field("q") q: String?,
        @Field("lat") lat_gps: Double?,
        @Field("long") long_gps: Double?,
        @Field("sort") sort: Int?,
        @Field("radius") radius: Int?,
        @Field("start_time") start_time: Long?,
        @Field("end_time") end_time: Long?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>>
}