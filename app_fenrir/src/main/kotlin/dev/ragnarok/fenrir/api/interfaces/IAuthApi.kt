package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.AnonymToken
import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.VKUrlResponse
import kotlinx.coroutines.flow.Flow

interface IAuthApi {
    fun directLogin(
        grantType: String?,
        clientId: Int,
        clientSecret: String?,
        username: String?,
        pass: String?,
        v: String?,
        twoFaSupported: Boolean,
        scope: String?,
        code: String?,
        captchaSid: String?,
        captchaKey: String?,
        forceSms: Boolean,
        libverify_support: Boolean
    ): Flow<LoginResponse>

    fun validatePhone(
        phone: String?,
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        sid: String?,
        v: String?,
        libverify_support: Boolean,
        allow_callreset: Boolean
    ): Flow<VKApiValidationResponse>

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
    ): Flow<VKUrlResponse>

    fun get_anonym_token(
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        v: String?,
        device_id: String?
    ): Flow<AnonymToken>
}