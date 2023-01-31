package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.VKUrlResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

class IAuthService : IServiceRest() {
    fun directLogin(
        grantType: String?,
        clientId: Int,
        clientSecret: String?,
        username: String?,
        password: String?,
        v: String?,
        twoFaSupported: Int?,
        scope: String?,
        smscode: String?,
        captchaSid: String?,
        captchaKey: String?,
        forceSms: Int?,
        device_id: String?,
        libverify_support: Int?
    ): Single<LoginResponse> {
        return rest.request(
            "token",
            form(
                "grant_type" to grantType,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "username" to username,
                "password" to password,
                "v" to v,
                "2fa_supported" to twoFaSupported,
                "scope" to scope,
                "code" to smscode,
                "captcha_sid" to captchaSid,
                "captcha_key" to captchaKey,
                "force_sms" to forceSms,
                "device_id" to device_id,
                "libverify_support" to libverify_support
            ), LoginResponse.serializer(), false
        )
    }

    // initiator = expired_token
    // gaid - ads_device_id
    // device_id - ads_android_id
    fun authByExchangeToken(
        clientId: Int,
        apiId: Int,
        exchangeToken: String,
        scope: String,
        initiator: String,
        deviceId: String?,
        sakVersion: String?,
        gaid: String?,
        v: String?
    ): Single<VKUrlResponse> {
        return rest.requestAndGetURLFromRedirects(
            "auth_by_exchange_token",
            form(
                "client_id" to clientId,
                "api_id" to apiId,
                "exchange_token" to exchangeToken,
                "scope" to scope,
                "initiator" to initiator,
                "device_id" to deviceId,
                "sak_version" to sakVersion,
                "gaid" to gaid,
                "v" to v
            )
        )
    }

    fun validatePhone(
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        sid: String?,
        v: String?
    ): Single<BaseResponse<VKApiValidationResponse>> {
        return rest.request(
            "auth.validatePhone",
            form(
                "api_id" to apiId,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "sid" to sid,
                "v" to v
            ),
            base(VKApiValidationResponse.serializer())
        )
    }
}