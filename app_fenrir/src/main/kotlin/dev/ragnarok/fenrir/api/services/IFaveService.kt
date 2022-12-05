package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.FavePageResponse
import dev.ragnarok.fenrir.api.model.response.FavePostsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IFaveService {
    @FormUrlEncoded
    @POST("fave.getPages")
    fun getPages(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("type") type: String?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<Items<FavePageResponse>>>

    @FormUrlEncoded
    @POST("fave.get")
    fun getVideos(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("item_type") item_type: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<Items<VKApiAttachments.Entry>>>

    @FormUrlEncoded
    @POST("fave.get")
    fun getArticles(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("item_type") item_type: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<Items<VKApiAttachments.Entry>>>

    @FormUrlEncoded
    @POST("articles.getOwnerPublished")
    fun getOwnerPublishedArticles(
        @Field("owner_id") owner_id: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("sort_by") sort_by: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<Items<VKApiArticle>>>

    @FormUrlEncoded
    @POST("fave.get")
    fun getPosts(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("item_type") item_type: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<FavePostsResponse>>

    @FormUrlEncoded
    @POST("fave.get")
    fun getLinks(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("item_type") item_type: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<Items<FaveLinkDto>>>

    @FormUrlEncoded
    @POST("fave.get")
    fun getProducts(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("item_type") item_type: String?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<Items<VKApiAttachments.Entry>>>

    @FormUrlEncoded
    @POST("fave.getPhotos")
    fun getPhotos(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiPhoto>>>

    @FormUrlEncoded
    @POST("fave.addLink")
    fun addLink(@Field("link") link: String?): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.addPage")
    fun addPage(
        @Field("user_id") userId: Int?,
        @Field("group_id") groupId: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.addVideo")
    fun addVideo(
        @Field("owner_id") owner_id: Int?,
        @Field("id") id: Int?,
        @Field("access_key") access_key: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.addArticle")
    fun addArticle(@Field("url") url: String?): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.addProduct")
    fun addProduct(
        @Field("id") id: Int,
        @Field("owner_id") owner_id: Int,
        @Field("access_key") access_key: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.addPost")
    fun addPost(
        @Field("owner_id") owner_id: Int?,
        @Field("id") id: Int?,
        @Field("access_key") access_key: String?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/fave.removePage
    @FormUrlEncoded
    @POST("fave.removePage")
    fun removePage(
        @Field("user_id") userId: Int?,
        @Field("group_id") groupId: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.removeLink")
    fun removeLink(@Field("link_id") linkId: String?): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.removeArticle")
    fun removeArticle(
        @Field("owner_id") owner_id: Int?,
        @Field("article_id") article_id: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.removeProduct")
    fun removeProduct(
        @Field("id") id: Int?,
        @Field("owner_id") owner_id: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.removePost")
    fun removePost(
        @Field("owner_id") owner_id: Int?,
        @Field("id") id: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("fave.removeVideo")
    fun removeVideo(
        @Field("owner_id") owner_id: Int?,
        @Field("id") id: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("execute")
    fun pushFirst(
        @Field("code") code: String?,
        @Field("owner_id") ownerId: Int
    ): Single<BaseResponse<Int>>
}