package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DefaultCommentsResponse
import dev.ragnarok.fenrir.api.model.response.SearchVideoResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IVideoService {
    @FormUrlEncoded
    @POST("video.getComments")
    fun getComments(
        @Field("owner_id") ownerId: Int?,
        @Field("video_id") videoId: Int,
        @Field("need_likes") needLikes: Int?,
        @Field("start_comment_id") startCommentId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("sort") sort: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<DefaultCommentsResponse>>

    @FormUrlEncoded
    @POST("video.add")
    fun addVideo(
        @Field("target_id") targetId: Int?,
        @Field("video_id") videoId: Int?,
        @Field("owner_id") ownerId: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("video.delete")
    fun deleteVideo(
        @Field("video_id") videoId: Int?,
        @Field("owner_id") ownerId: Int?,
        @Field("target_id") targetId: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("video.getAlbums")
    fun getAlbums(
        @Field("owner_id") ownerId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?,
        @Field("need_system") needSystem: Int?
    ): Single<BaseResponse<Items<VKApiVideoAlbum>>>

    @FormUrlEncoded
    @POST("video.getAlbumsByVideo")
    fun getAlbumsByVideo(
        @Field("target_id") target_id: Int?,
        @Field("owner_id") owner_id: Int?,
        @Field("video_id") video_id: Int?,
        @Field("extended") extended: Int?
    ): Single<BaseResponse<Items<VKApiVideoAlbum>>>

    @FormUrlEncoded
    @POST("video.search")
    fun search(
        @Field("q") query: String?,
        @Field("sort") sort: Int?,
        @Field("hd") hd: Int?,
        @Field("adult") adult: Int?,
        @Field("filters") filters: String?,
        @Field("search_own") searchOwn: Int?,
        @Field("offset") offset: Int?,
        @Field("longer") longer: Int?,
        @Field("shorter") shorter: Int?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?
    ): Single<BaseResponse<SearchVideoResponse>>

    @FormUrlEncoded
    @POST("video.restoreComment")
    fun restoreComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("video.deleteComment")
    fun deleteComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("video.get")
    operator fun get(
        @Field("owner_id") ownerId: Int?,
        @Field("videos") videos: String?,
        @Field("album_id") albumId: Int?,
        @Field("count") count: Int?,
        @Field("offset") offset: Int?,
        @Field("extended") extended: Int?
    ): Single<BaseResponse<Items<VKApiVideo>>>

    @FormUrlEncoded
    @POST("video.createComment")
    fun createComment(
        @Field("owner_id") ownerId: Int?,
        @Field("video_id") videoId: Int,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?,
        @Field("from_group") fromGroup: Int?,
        @Field("reply_to_comment") replyToComment: Int?,
        @Field("sticker_id") stickerId: Int?,
        @Field("guid") uniqueGeneratedId: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("video.editComment")
    fun editComment(
        @Field("owner_id") ownerId: Int?,
        @Field("comment_id") commentId: Int,
        @Field("message") message: String?,
        @Field("attachments") attachments: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("video.edit")
    fun edit(
        @Field("owner_id") ownerId: Int?,
        @Field("video_id") video_id: Int,
        @Field("name") name: String?,
        @Field("desc") desc: String?
    ): Single<BaseResponse<Int>>
}