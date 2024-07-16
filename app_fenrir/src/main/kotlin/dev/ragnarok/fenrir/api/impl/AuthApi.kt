package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants.DEVICE_COUNTRY_CODE
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.AuthException
import dev.ragnarok.fenrir.api.CaptchaNeedException
import dev.ragnarok.fenrir.api.IDirectLoginServiceProvider
import dev.ragnarok.fenrir.api.NeedValidationException
import dev.ragnarok.fenrir.api.interfaces.IAuthApi
import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.BaseResponse
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
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        sid: String?,
        v: String?,
        libverify_support: Boolean
    ): Flow<VKApiValidationResponse> {
        return service.provideAuthService()
            .flatMapConcat {
                it.validatePhone(
                    apiId, clientId, clientSecret, sid, v, getDeviceId(
                        provideApplicationContext()
                    ), if (libverify_support) 1 else null, DEVICE_COUNTRY_CODE
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

    companion object {
        fun <T : Any> extractResponseWithErrorHandling(): (BaseResponse<T>) -> T = { err ->
            err.error?.let { throw ApiException(it) }
                ?: (err.response
                    ?: throw NullPointerException("VK return null response"))
        }
    }
}