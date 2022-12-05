package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiFriendList
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.DeleteFriendResponse
import dev.ragnarok.fenrir.api.model.response.MutualFriendsResponse
import dev.ragnarok.fenrir.api.model.response.OnlineFriendsResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IFriendsService {
    @FormUrlEncoded
    @POST("execute")
    fun getOnline(@Field("code") code: String?): Single<BaseResponse<OnlineFriendsResponse>>

    /*@FormUrlEncoded
    @POST("execute")
    Single<BaseResponse<FriendsWithCountersResponse>> getWithMyCounters(@Field("code") String code);*/
    @FormUrlEncoded
    @POST("friends.get")
    operator fun get(
        @Field("user_id") userId: Int?,
        @Field("order") order: String?,
        @Field("list_id") listId: Int?,
        @Field("count") count: Int?,
        @Field("offset") offset: Int?,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?
    ): Single<BaseResponse<Items<VKApiUser>>>

    @FormUrlEncoded
    @POST("friends.getRecommendations")
    fun getRecommendations(
        @Field("count") count: Int?,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?
    ): Single<BaseResponse<Items<VKApiUser>>>

    //https://vk.com/dev/friends.getLists
    @FormUrlEncoded
    @POST("friends.getLists")
    fun getLists(
        @Field("user_id") userId: Int?,
        @Field("return_system") returnSystem: Int?
    ): Single<BaseResponse<Items<VKApiFriendList>>>

    //https://vk.com/dev/friends.delete
    @FormUrlEncoded
    @POST("friends.delete")
    fun delete(@Field("user_id") userId: Int): Single<BaseResponse<DeleteFriendResponse>>

    //https://vk.com/dev/friends.add
    @FormUrlEncoded
    @POST("friends.add")
    fun add(
        @Field("user_id") userId: Int,
        @Field("text") text: String?,
        @Field("follow") follow: Int?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/friends.search
    @FormUrlEncoded
    @POST("friends.search")
    fun search(
        @Field("user_id") userId: Int,
        @Field("q") query: String?,
        @Field("fields") fields: String?,
        @Field("name_case") nameCase: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiUser>>>

    @FormUrlEncoded
    @POST("execute")
    fun getMutual(@Field("code") code: String?): Single<BaseResponse<MutualFriendsResponse>>

    //https://vk.com/dev/friends.getByPhones
    @FormUrlEncoded
    @POST("friends.getByPhones")
    fun getByPhones(
        @Field("phones") phones: String?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<List<VKApiUser>>>
}