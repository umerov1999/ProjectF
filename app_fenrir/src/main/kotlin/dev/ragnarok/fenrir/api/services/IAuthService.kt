package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.AnonymToken
import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.VKUrlResponse
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

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
        libverify_support: Int?,
        lang: String?
    ): Flow<LoginResponse> {
        return rest.request(
            "token",
            form(
                "libverify_support" to libverify_support,
                "password" to password,
                "code" to smscode,
                "grant_type" to grantType,
                "2fa_supported" to twoFaSupported,
                "v" to v,
                "scope" to scope,
                "client_secret" to clientSecret,
                "client_id" to clientId,
                "username" to username,
                "captcha_sid" to captchaSid,
                "captcha_key" to captchaKey,
                "force_sms" to forceSms,
                "device_id" to device_id,
                "lang" to lang,
                "https" to 1
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
        v: String?,
        lang: String?
    ): Flow<VKUrlResponse> {
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
                "v" to v,
                "lang" to lang,
                "https" to 1
            )
        )
    }

    fun validatePhone(
        phone: String?,
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        sid: String?,
        v: String?,
        device_id: String?,
        libverify_support: Int?,
        allow_callreset: Int?,
        lang: String?
    ): Flow<BaseResponse<VKApiValidationResponse>> {
        return rest.request(
            "auth.validatePhone",
            form(
                "libverify_support" to libverify_support,
                "allow_callreset" to allow_callreset,
                "phone" to phone,
                "api_id" to apiId,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "sid" to sid,
                "v" to v,
                "device_id" to device_id,
                "lang" to lang,
                "https" to 1
            ),
            base(VKApiValidationResponse.serializer())
        )
    }

    fun get_anonym_token(
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        v: String?,
        device_id: String?,
        lang: String?
    ): Flow<AnonymToken> {
        return rest.request(
            "get_anonym_token",
            form(
                "api_id" to apiId,
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "v" to v,
                "device_id" to device_id,
                "lang" to lang,
                "https" to 1
            ), AnonymToken.serializer()
        )
    }

    /*
    fun refreshTokens(
        clientId: Int,
        apiId: Int,
        clientSecret: String,
        device_id: String,
        lang: String?,
        scope: String,
        initiator: String?,
        exchange_token: String,
        active_index: Int?,
        v: String?
    ): Single<LoginResponse> {
        return rest.request(
            "auth.refreshTokens",
            form(
                "v" to v,
                "scope" to scope,
                "client_secret" to clientSecret,
                "client_id" to clientId,
                "apiId" to apiId,
                "initiator" to initiator,
                "device_id" to device_id,
                "exchange_tokens" to exchange_token,
                "active_index" to active_index,
                "lang" to lang,
                "https" to 1
            ), RefreshExpiredTokenResponse.serializer(), false
        )
    }

    {
    "response": {
        "success": [{
            "index": 0,
            "user_id": 22*,
            "banned": false,
            "access_token": {
                "token": "vk1.a.",
                "expires_in": 0
            },
            "webview_access_token": {
                "token": "",
                "expires_in": 1695726230
            },
            "webview_refresh_token": {
                "token": "",
                "expires_in": 1698231830
            }
        }],
        "errors": [{
            "index": 1,
            "code": 5,
            "description": "User authorization failed"
        }]
    }
}
     */
}