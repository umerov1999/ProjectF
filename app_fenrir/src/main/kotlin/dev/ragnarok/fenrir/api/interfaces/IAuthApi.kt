package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.AnonymTokenResponse
import dev.ragnarok.fenrir.api.model.response.GetAuthCodeStatusResponse
import dev.ragnarok.fenrir.api.model.response.LoginResponse
import dev.ragnarok.fenrir.api.model.response.SetAuthCodeStatusResponse
import dev.ragnarok.fenrir.api.model.response.VKUrlResponse
import kotlinx.coroutines.flow.Flow

interface IAuthApi {
    @CheckResult
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

    @CheckResult
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

    @CheckResult
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

    @CheckResult
    fun get_anonym_token(
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        v: String?,
        device_id: String?
    ): Flow<AnonymTokenResponse>

    @CheckResult
    fun setAuthCodeStatus(
        auth_code: String?,
        apiId: Int,
        device_id: String?,
        accessToken: String?,
        v: String?
    ): Flow<SetAuthCodeStatusResponse>

    @CheckResult
    fun getAuthCodeStatus(
        auth_code: String?,
        apiId: Int,
        device_id: String?,
        accessToken: String?,
        v: String?
    ): Flow<GetAuthCodeStatusResponse>
}