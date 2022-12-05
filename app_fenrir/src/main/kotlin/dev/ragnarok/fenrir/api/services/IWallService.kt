package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.*
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IWallService {
    //https://vk.com/dev/wall.search
    @FormUrlEncoded
    @POST("wall.search")
    fun search(
        @Field("owner_id") ownerId: Int?,
        @Field("domain") domain: String?,
        @Field("query") query: String?,
        @Field("owners_only") ownersOnly: Int?,
        @Field("count") count: Int?,
        @Field("offset") offset: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<WallSearchResponse>>

    //https://vk.com/dev/wall.edit
    @FormUrlEncoded
    @POST("wall.edit")
    fun edit(
        @Field("owner_id") ownerId: Int?,
        @Field("post_id") postId: Int?,
        @Field("friends_only") friendsOnly: Int?,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?,
        @Field("services") services: String?,
        @Field("signed") signed: Int?,
        @Field("publish_date") publishDate: Long?,
        @Field("lat") latitude: Double?,
        @Field("long") longitude: Double?,
        @Field("place_id") placeId: Int?,
        @Field("mark_as_ads") markAsAds: Int?
    ): Single<BaseResponse<WallEditResponse>>

    //https://vk.com/dev/wall.pin
    @FormUrlEncoded
    @POST("wall.pin")
    fun pin(
        @Field("owner_id") ownerId: Int?,
        @Field("post_id") postId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/wall.unpin
    @FormUrlEncoded
    @POST("wall.unpin")
    fun unpin(
        @Field("owner_id") ownerId: Int?,
        @Field("post_id") postId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/wall.repost
    @FormUrlEncoded
    @POST("wall.repost")
    fun repost(
        @Field("object") `object`: String?,
        @Field("message") message: String?,
        @Field("group_id") groupId: Int?,
        @Field("mark_as_ads") markAsAds: Int?
    ): Single<BaseResponse<RepostReponse>>

    //https://vk.com/dev/wall.post
    @FormUrlEncoded
    @POST("wall.post")
    fun post(
        @Field("owner_id") ownerId: Int?,
        @Field("friends_only") friendsOnly: Int?,
        @Field("from_group") fromGroup: Int?,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?,
        @Field("services") services: String?,
        @Field("signed") signed: Int?,
        @Field("publish_date") publishDate: Long?,
        @Field("lat") latitude: Double?,
        @Field("long") longitude: Double?,
        @Field("place_id") placeId: Int?,
        @Field("post_id") postId: Int?,
        @Field("guid") guid: Int?,
        @Field("mark_as_ads") markAsAds: Int?,
        @Field("ads_promoted_stealth") adsPromotedStealth: Int?
    ): Single<BaseResponse<PostCreateResponse>>

    /**
     * Deletes a post from a user wall or community wall.
     *
     * @param ownerId User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param postId  ID of the post to be deleted
     * @return 1
     */
    @FormUrlEncoded
    @POST("wall.delete")
    fun delete(
        @Field("owner_id") ownerId: Int?,
        @Field("post_id") postId: Int
    ): Single<BaseResponse<Int>>

    /**
     * Restores a comment deleted from a user wall or community wall.
     *
     * @param ownerId   User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param commentId Comment ID.
     * @return 1
     */
    @FormUrlEncoded
    @POST("wall.restoreComment")
    fun restoreComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    /**
     * Deletes a comment on a post on a user wall or community wall.
     *
     * @param ownerId   User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param commentId Comment ID.
     * @return 1
     */
    @FormUrlEncoded
    @POST("wall.deleteComment")
    fun deleteComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    /**
     * Restores a post deleted from a user wall or community wall.
     *
     * @param ownerId User ID or community ID from whose wall the post was deleted.
     * Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param postId  ID of the post to be restored.
     * @return 1
     */
    @FormUrlEncoded
    @POST("wall.restore")
    fun restore(
        @Field("owner_id") ownerId: Int?,
        @Field("post_id") postId: Int
    ): Single<BaseResponse<Int>>

    /**
     * Edits a comment on a user wall or community wall.
     *
     * @param ownerId     User ID or community ID. Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param commentId   Comment ID.
     * @param message     New comment text.
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
    @POST("wall.editComment")
    fun editComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("wall.createComment")
    fun createComment(
        @Field("owner_id") ownerId: Int?,
        @Field("post_id") postId: Int,
        @Field("from_group") fromGroup: Int?,
        @Field("message") message: String?,
        @Field("reply_to_comment") replyToComment: Int?,
        @Field("attachments") attachments: String?,
        @Field("sticker_id") stickerId: Int?,
        @Field("guid") generatedUniqueId: Int?
    ): Single<BaseResponse<CommentCreateResponse>>

    //https://vk.com/dev/wall.getComments
    @FormUrlEncoded
    @POST("wall.getComments")
    fun getComments(
        @Field("owner_id") ownerId: Int?,
        @Field("post_id") postId: Int,
        @Field("need_likes") needLikes: Int?,
        @Field("start_comment_id") startCommentId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("sort") sort: String?,
        @Field("extended") extended: Int?,
        @Field("thread_items_count") thread_items_count: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>>

    @FormUrlEncoded
    @POST("wall.get")
    operator fun get(
        @Field("owner_id") ownerId: Int?,
        @Field("domain") domain: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("filter") filter: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<WallResponse>>

    @FormUrlEncoded
    @POST("wall.getById")
    fun getById(
        @Field("posts") ids: String?,
        @Field("extended") extended: Int?,
        @Field("copy_history_depth") copyHistoryDepth: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<PostsResponse>>

    @POST("wall.reportComment")
    @FormUrlEncoded
    fun reportComment(
        @Field("owner_id") owner_id: Int?,
        @Field("comment_id") comment_id: Int?,
        @Field("reason") reason: Int?
    ): Single<BaseResponse<Int>>

    @POST("wall.reportPost")
    @FormUrlEncoded
    fun reportPost(
        @Field("owner_id") owner_id: Int?,
        @Field("post_id") post_id: Int?,
        @Field("reason") reason: Int?
    ): Single<BaseResponse<Int>>

    @POST("wall.subscribe")
    @FormUrlEncoded
    fun subscribe(@Field("owner_id") owner_id: Int?): Single<BaseResponse<Int>>

    @POST("wall.unsubscribe")
    @FormUrlEncoded
    fun unsubscribe(@Field("owner_id") owner_id: Int?): Single<BaseResponse<Int>>
}