package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Constants.DEVICE_COUNTRY_CODE
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.AuthException
import dev.ragnarok.fenrir.api.CaptchaNeedException
import dev.ragnarok.fenrir.api.IDirectLoginServiceProvider
import dev.ragnarok.fenrir.api.NeedValidationException
import dev.ragnarok.fenrir.api.interfaces.IAuthApi
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.AnonymTokenResponse
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.GetAuthCodeStatusResponse
import dev.ragnarok.fenrir.api.model.response.LoginResponse
import dev.ragnarok.fenrir.api.model.response.SetAuthCodeStatusResponse
import dev.ragnarok.fenrir.api.model.response.VKUrlResponse
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getDeviceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map

class AuthApi(private val service: IDirectLoginServiceProvider) : IAuthApi {
    override fun directLogin(
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
    ): Flow<LoginResponse> {
        return service.provideAuthService()
            .flatMapConcat {
                it.directLogin(
                    grantType,
                    clientId,
                    clientSecret,
                    username,
                    pass,
                    v,
                    if (twoFaSupported) 1 else null,
                    scope,
                    code,
                    captchaSid,
                    captchaKey,
                    if (forceSms) 1 else null,
                    getDeviceId(
                        Constants.DEFAULT_ACCOUNT_TYPE,
                        provideApplicationContext()
                    ),
                    if (libverify_support) 1 else null,
                    DEVICE_COUNTRY_CODE
                )
                    .map { response ->
                        when {
                            "need_captcha".equals(response.error, ignoreCase = true) -> {
                                throw CaptchaNeedException(
                                    response.captchaSid,
                                    response.captchaImg
                                )
                            }

                            "need_validation".equals(response.error, ignoreCase = true) -> {
                                throw NeedValidationException(
                                    username,
                                    response.validationType,
                                    response.redirect_uri,
                                    response.validation_sid,
                                    response.errorDescription
                                )
                            }

                            response.error.nonNullNoEmpty() -> {
                                throw AuthException(
                                    response.error.orEmpty(),
                                    response.errorDescription
                                )
                            }

                            else -> response
                        }
                    }
            }
    }

    override fun validatePhone(
        phone: String?,
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        sid: String?,
        v: String?,
        libverify_support: Boolean,
        allow_callreset: Boolean
    ): Flow<VKApiValidationResponse> {
        return service.provideAuthService()
            .flatMapConcat {
                it.validatePhone(
                    phone,
                    apiId,
                    clientId,
                    clientSecret,
                    sid,
                    v,
                    getDeviceId(
                        Constants.DEFAULT_ACCOUNT_TYPE,
                        provideApplicationContext()
                    ),
                    if (libverify_support) 1 else null,
                    if (allow_callreset) 1 else null,
                    DEVICE_COUNTRY_CODE
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun authByExchangeToken(
        clientId: Int,
        apiId: Int,
        exchangeToken: String,
        scope: String,
        initiator: String,
        deviceId: String?,
        sakVersion: String?,
        gaid: String?,
        v: String?
    ): Flow<VKUrlResponse> {
        return service.provideAuthService()
            .flatMapConcat {
                it.authByExchangeToken(
                    clientId,
                    apiId,
                    exchangeToken,
                    scope,
                    initiator,
                    deviceId,
                    sakVersion,
                    gaid,
                    v,
                    DEVICE_COUNTRY_CODE
                )
                    .map { s ->
                        if (s.error != null) {
                            throw AuthException(s.error.orEmpty(), s.errorDescription)
                        } else {
                            s
                        }
                    }
            }
    }

    override fun get_anonym_token(
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        v: String?,
        device_id: String?
    ): Flow<AnonymTokenResponse> {
        return service.provideAuthService()
            .flatMapConcat {
                it.get_anonym_token(
                    apiId,
                    clientId,
                    clientSecret,
                    v,
                    device_id,
                    DEVICE_COUNTRY_CODE
                )
                    .map { res ->
                        if (res.error != null) {
                            throw AuthException(
                                res.error.orEmpty(),
                                res.errorDescription
                            )
                        } else {
                            res
                        }
                    }
            }
    }

    override fun setAuthCodeStatus(
        auth_code: String?,
        apiId: Int,
        device_id: String?,
        accessToken: String?,
        v: String?
    ): Flow<SetAuthCodeStatusResponse> {
        return service.provideAuthService()
            .flatMapConcat {
                it.setAuthCodeStatus(
                    auth_code,
                    apiId,
                    device_id,
                    accessToken,
                    DEVICE_COUNTRY_CODE,
                    v
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getAuthCodeStatus(
        auth_code: String?,
        apiId: Int,
        device_id: String?,
        accessToken: String?,
        v: String?
    ): Flow<GetAuthCodeStatusResponse> {
        return service.provideAuthService()
            .flatMapConcat {
                it.getAuthCodeStatus(
                    auth_code,
                    apiId,
                    device_id,
                    accessToken,
                    DEVICE_COUNTRY_CODE,
                    v
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    companion object {
        fun <T : Any> extractResponseWithErrorHandling(): (BaseResponse<T>) -> T = { err ->
            err.error?.let { throw ApiException(it) }
                ?: (err.response
                    ?: throw NullPointerException("VK return null response"))
        }
    }
}