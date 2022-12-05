package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.GroupLongpollServer
import dev.ragnarok.fenrir.api.model.response.GroupWallInfoResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IGroupsService {
    @FormUrlEncoded
    @POST("groups.editManager")
    fun editManager(
        @Field("group_id") groupId: Int,
        @Field("user_id") userId: Int,
        @Field("role") role: String?,
        @Field("is_contact") isContact: Int?,
        @Field("contact_position") contactPosition: String?,
        @Field("contact_email") contactEmail: String?,
        @Field("contact_phone") contactPhone: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("groups.edit")
    fun edit(
        @Field("group_id") groupId: Int,
        @Field("title") title: String?,
        @Field("description") description: String?,
        @Field("screen_name") screen_name: String?,
        @Field("access") access: Int?,
        @Field("website") website: String?,
        //@Field("public_category") public_category: Int?,
        //@Field("public_subcategory") public_subcategory: Int?,
        @Field("public_date") public_date: String?,
        @Field("age_limits") age_limits: Int?,
        @Field("obscene_filter") obscene_filter: Int?,
        @Field("obscene_stopwords") obscene_stopwords: Int?,
        @Field("obscene_words") obscene_words: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("groups.unban")
    fun unban(
        @Field("group_id") groupId: Int,
        @Field("owner_id") ownerId: Int
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("market.getAlbums")
    fun getMarketAlbums(
        @Field("owner_id") owner_id: Int,
        @Field("offset") offset: Int,
        @Field("count") count: Int
    ): Single<BaseResponse<Items<VKApiMarketAlbum>>>

    @FormUrlEncoded
    @POST("market.get")
    fun getMarket(
        @Field("owner_id") owner_id: Int,
        @Field("album_id") album_id: Int?,
        @Field("offset") offset: Int,
        @Field("count") count: Int,
        @Field("extended") extended: Int?
    ): Single<BaseResponse<Items<VKApiMarket>>>

    @FormUrlEncoded
    @POST("market.getServices")
    fun getMarketServices(
        @Field("owner_id") owner_id: Int,
        @Field("offset") offset: Int,
        @Field("count") count: Int,
        @Field("extended") extended: Int?
    ): Single<BaseResponse<Items<VKApiMarket>>>

    @FormUrlEncoded
    @POST("market.getById")
    fun getMarketById(
        @Field("item_ids") item_ids: String?,
        @Field("extended") extended: Int?
    ): Single<BaseResponse<Items<VKApiMarket>>>

    @POST("groups.ban")
    @FormUrlEncoded
    fun ban(
        @Field("group_id") groupId: Int,
        @Field("owner_id") ownerId: Int,
        @Field("end_date") endDate: Long?,
        @Field("reason") reason: Int?,
        @Field("comment") comment: String?,
        @Field("comment_visible") commentVisible: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("groups.getSettings")
    fun getSettings(@Field("group_id") groupId: Int): Single<BaseResponse<GroupSettingsDto>>

    //https://vk.com/dev/groups.getBanned
    @FormUrlEncoded
    @POST("groups.getBanned")
    fun getBanned(
        @Field("group_id") groupId: Int,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?,
        @Field("user_id") userId: Int?
    ): Single<BaseResponse<Items<VKApiBanned>>>

    @FormUrlEncoded
    @POST("execute")
    fun getGroupWallInfo(
        @Field("code") code: String?,
        @Field("group_id") groupId: String?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<GroupWallInfoResponse>>

    @FormUrlEncoded
    @POST("groups.getMembers")
    fun getMembers(
        @Field("group_id") groupId: String?,
        @Field("sort") sort: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("fields") fields: String?,
        @Field("filter") filter: String?
    ): Single<BaseResponse<Items<VKApiUser>>>

    //https://vk.com/dev/groups.search
    @FormUrlEncoded
    @POST("groups.search")
    fun search(
        @Field("q") query: String?,
        @Field("type") type: String?,
        @Field("fields") fields: String?,
        @Field("country_id") countryId: Int?,
        @Field("city_id") cityId: Int?,
        @Field("future") future: Int?,
        @Field("market") market: Int?,
        @Field("sort") sort: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiCommunity>>>

    @FormUrlEncoded
    @POST("groups.getLongPollServer")
    fun getLongPollServer(@Field("group_id") groupId: Int): Single<BaseResponse<GroupLongpollServer>>

    //https://vk.com/dev/groups.leave
    @FormUrlEncoded
    @POST("groups.leave")
    fun leave(@Field("group_id") groupId: Int): Single<BaseResponse<Int>>

    //https://vk.com/dev/groups.join
    @FormUrlEncoded
    @POST("groups.join")
    fun join(
        @Field("group_id") groupId: Int,
        @Field("not_sure") notSure: Int?
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/groups.get
    @FormUrlEncoded
    @POST("groups.get")
    operator fun get(
        @Field("user_id") userId: Int?,
        @Field("extended") extended: Int?,
        @Field("filter") filter: String?,
        @Field("fields") fields: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiCommunity>>>

    /**
     * Returns information about communities by their IDs.
     *
     * @param groupIds IDs or screen names of communities.
     * List of comma-separated words
     * @param groupId  ID or screen name of the community
     * @param fields   Group fields to return. List of comma-separated words
     * @return an array of objects describing communities
     */
    @FormUrlEncoded
    @POST("groups.getById")
    fun getById(
        @Field("group_ids") groupIds: String?,
        @Field("group_id") groupId: String?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<List<VKApiCommunity>>>

    @FormUrlEncoded
    @POST("groups.getChats")
    fun getChats(
        @Field("group_id") groupId: Int,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiGroupChats>>>
}