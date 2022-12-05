package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiCheckedLink
import dev.ragnarok.fenrir.api.model.VKApiShortLink
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.ResolveDomailResponse
import dev.ragnarok.fenrir.api.model.response.VKApiChatResponse
import dev.ragnarok.fenrir.api.model.response.VKApiLinkResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IUtilsService {
    @FormUrlEncoded
    @POST("utils.resolveScreenName")
    fun resolveScreenName(@Field("screen_name") screenName: String?): Single<BaseResponse<ResolveDomailResponse>>

    @FormUrlEncoded
    @POST("utils.getShortLink")
    fun getShortLink(
        @Field("url") url: String?,
        @Field("private") t_private: Int?
    ): Single<BaseResponse<VKApiShortLink>>

    @FormUrlEncoded
    @POST("utils.getLastShortenedLinks")
    fun getLastShortenedLinks(
        @Field("count") count: Int?,
        @Field("offset") offset: Int?
    ): Single<BaseResponse<Items<VKApiShortLink>>>

    @FormUrlEncoded
    @POST("utils.deleteFromLastShortened")
    fun deleteFromLastShortened(@Field("key") key: String?): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("utils.checkLink")
    fun checkLink(@Field("url") url: String?): Single<BaseResponse<VKApiCheckedLink>>

    @FormUrlEncoded
    @POST("messages.joinChatByInviteLink")
    fun joinChatByInviteLink(@Field("link") link: String?): Single<BaseResponse<VKApiChatResponse>>

    @FormUrlEncoded
    @POST("messages.getInviteLink")
    fun getInviteLink(
        @Field("peer_id") peer_id: Int?,
        @Field("reset") reset: Int?
    ): Single<BaseResponse<VKApiLinkResponse>>

    @FormUrlEncoded
    @POST("execute")
    fun customScript(@Field("code") code: String?): Single<BaseResponse<Int>>
}