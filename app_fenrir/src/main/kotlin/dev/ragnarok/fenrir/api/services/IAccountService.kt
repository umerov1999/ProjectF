package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.CountersDto
import dev.ragnarok.fenrir.api.model.RefreshToken
import dev.ragnarok.fenrir.api.model.VKApiProfileInfo
import dev.ragnarok.fenrir.api.model.VKApiProfileInfoResponse
import dev.ragnarok.fenrir.api.model.response.*
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface IAccountService {
    @POST("account.ban")
    @FormUrlEncoded
    fun ban(@Field("owner_id") owner_id: Int): Single<BaseResponse<Int>>

    @POST("account.unban")
    @FormUrlEncoded
    fun unban(@Field("owner_id") owner_id: Int): Single<BaseResponse<Int>>

    @POST("account.getBanned")
    @FormUrlEncoded
    fun getBanned(
        @Field("count") count: Int?,
        @Field("offset") offset: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<AccountsBannedResponse>>
    //https://vk.com/dev/account.getCounters
    /**
     * @param filter friends — новые заявки в друзья;
     * friends_suggestions — предлагаемые друзья;
     * messages — новые сообщения;
     * photos — новые отметки на фотографиях;
     * videos — новые отметки на видеозаписях;
     * gifts — подарки;
     * events — события;
     * groups — сообщества;
     * notifications — ответы;
     * sdk — запросы в мобильных играх;
     * app_requests — уведомления от приложений.
     */
    @POST("account.getCounters")
    @FormUrlEncoded
    fun getCounters(@Field("filter") filter: String?): Single<BaseResponse<CountersDto>>

    //https://vk.com/dev/account.unregisterDevice
    @FormUrlEncoded
    @POST("account.unregisterDevice")
    fun unregisterDevice(@Field("device_id") deviceId: String?): Single<BaseResponse<Int>>

    //https://vk.com/dev/account.registerDevice
    @FormUrlEncoded
    @POST("account.registerDevice")
    fun registerDevice(
        @Field("token") token: String?,
        @Field("pushes_granted") pushes_granted: Int?,
        @Field("app_version") app_version: String?,
        @Field("push_provider") push_provider: String?,
        @Field("companion_apps") companion_apps: String?,
        @Field("type") type: Int?,
        @Field("device_model") deviceModel: String?,
        @Field("device_id") deviceId: String?,
        @Field("system_version") systemVersion: String?,
        @Field("settings") settings: String?
    ): Single<BaseResponse<Int>>

    /**
     * Marks a current user as offline.
     *
     * @return In case of success returns 1.
     */
    @GET("account.setOffline")
    fun setOffline(): Single<BaseResponse<Int>>

    @get:GET("account.getProfileInfo")
    val profileInfo: Single<BaseResponse<VKApiProfileInfo>>

    @get:GET("account.getPushSettings")
    val pushSettings: Single<BaseResponse<PushSettingsResponse>>

    @FormUrlEncoded
    @POST("account.saveProfileInfo")
    fun saveProfileInfo(
        @Field("first_name") first_name: String?,
        @Field("last_name") last_name: String?,
        @Field("maiden_name") maiden_name: String?,
        @Field("screen_name") screen_name: String?,
        @Field("bdate") bdate: String?,
        @Field("home_town") home_town: String?,
        @Field("sex") sex: Int?
    ): Single<BaseResponse<VKApiProfileInfoResponse>>

    @FormUrlEncoded
    @POST("auth.refreshToken")
    fun refreshToken(
        @Field("receipt") receipt: String?,
        @Field("receipt2") receipt2: String?,
        @Field("nonce") nonce: String?,
        @Field("timestamp") timestamp: Long?
    ): Single<BaseResponse<RefreshToken>>

    @FormUrlEncoded
    @POST("account.importMessagesContacts")
    fun importMessagesContacts(
        @Field("contacts") contacts: String?
    ): Single<VkResponse>

    @GET("account.resetMessagesContacts")
    fun resetMessagesContacts(): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("account.getContactList")
    fun getContactList(
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("extended") extended: Int?,
        @Field("fields") fields: String?
    ): Single<BaseResponse<ContactsResponse>>
}