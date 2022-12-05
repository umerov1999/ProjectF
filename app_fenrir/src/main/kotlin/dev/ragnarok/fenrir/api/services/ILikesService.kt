package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.LikeResponse
import dev.ragnarok.fenrir.api.model.response.LikesListResponse
import dev.ragnarok.fenrir.api.model.response.isLikeResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ILikesService {
    //https://vk.com/dev/likes.getList
    @FormUrlEncoded
    @POST("likes.getList")
    fun getList(
        @Field("type") type: String?,
        @Field("owner_id") ownerId: Int?,
        @Field("item_id") itemId: Int?,
        @Field("page_url") pageUrl: String?,
        @Field("filter") filter: String?,
        @Field("friends_only") friendsOnly: Int?,
        @Field("extended") extended: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("skip_own") skipOwn: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<LikesListResponse>>

    //https://vk.com/dev/likes.delete
    @FormUrlEncoded
    @POST("likes.delete")
    fun delete(
        @Field("type") type: String?,
        @Field("owner_id") ownerId: Int?,
        @Field("item_id") itemId: Int,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<LikeResponse>>

    //https://vk.com/dev/likes.add
    @FormUrlEncoded
    @POST("likes.add")
    fun add(
        @Field("type") type: String?,
        @Field("owner_id") ownerId: Int?,
        @Field("item_id") itemId: Int,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<LikeResponse>>

    //https://vk.com/dev/likes.isLiked
    @FormUrlEncoded
    @POST("likes.isLiked")
    fun isLiked(
        @Field("type") type: String?,
        @Field("owner_id") ownerId: Int?,
        @Field("item_id") itemId: Int
    ): Single<BaseResponse<isLikeResponse>>

    @FormUrlEncoded
    @POST("execute")
    fun checkAndAddLike(
        @Field("code") code: String?,
        @Field("type") type: String?,
        @Field("owner_id") ownerId: Int?,
        @Field("item_id") itemId: Int,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<Int>>
}