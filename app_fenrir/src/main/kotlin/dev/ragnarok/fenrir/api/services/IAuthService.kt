package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IAuthService {
    @FormUrlEncoded
    @POST("token")
    fun directLogin(
        @Field("grant_type") grantType: String?,
        @Field("client_id") clientId: Int,
        @Field("client_secret") clientSecret: String?,
        @Field("username") username: String?,
        @Field("password") password: String?,
        @Field("v") v: String?,
        @Field("2fa_supported") twoFaSupported: Int?,
        @Field("scope") scope: String?,
        @Field("code") smscode: String?,
        @Field("captcha_sid") captchaSid: String?,
        @Field("captcha_key") captchaKey: String?,
        @Field("force_sms") forceSms: Int?,
        @Field("device_id") device_id: String?,
        @Field("libverify_support") libverify_support: Int?
    ): Single<LoginResponse>

    @FormUrlEncoded
    @POST("auth.validatePhone")
    fun validatePhone(
        @Field("api_id") apiId: Int,
        @Field("client_id") clientId: Int,
        @Field("client_secret") clientSecret: String?,
        @Field("sid") sid: String?,
        @Field("v") v: String?
    ): Single<BaseResponse<VKApiValidationResponse>>
}