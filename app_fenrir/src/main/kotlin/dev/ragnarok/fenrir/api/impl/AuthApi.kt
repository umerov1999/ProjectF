package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants.DEVICE_COUNTRY_CODE
import dev.ragnarok.fenrir.Includes.provideApplicationContext
import dev.ragnarok.fenrir.api.ApiException
import dev.ragnarok.fenrir.api.AuthException
import dev.ragnarok.fenrir.api.CaptchaNeedException
import dev.ragnarok.fenrir.api.IDirectLoginSeviceProvider
import dev.ragnarok.fenrir.api.NeedValidationException
import dev.ragnarok.fenrir.api.interfaces.IAuthApi
import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.VKUrlResponse
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.util.Utils.getDeviceId
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.functions.Function

class AuthApi(private val service: IDirectLoginSeviceProvider) : IAuthApi {
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
    ): Single<LoginResponse> {
        return service.provideAuthService()
            .flatMap { service ->
                service
                    .directLogin(
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
                    .flatMap { response ->
                        when {
                            "need_captcha".equals(response.error, ignoreCase = true) -> {
                                Single.error(
                                    CaptchaNeedException(
                                        response.captchaSid,
                                        response.captchaImg
                                    )
                                )
                            }

                            "need_validation".equals(response.error, ignoreCase = true) -> {
                                Single.error(
                                    NeedValidationException(
                                        response.validationType,
                                        response.redirect_uri,
                                        response.validation_sid,
                                        response.errorDescription
                                    )
                                )
                            }

                            response.error.nonNullNoEmpty() -> {
                                Single.error(
                                    AuthException(
                                        response.error.orEmpty(),
                                        response.errorDescription
                                    )
                                )
                            }

                            else -> Single.just(response)
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
    ): Single<VKApiValidationResponse> {
        return service.provideAuthService()
            .flatMap { service ->
                service
                    .validatePhone(
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
    ): Single<VKUrlResponse> {
        return service.provideAuthService()
            .flatMap { service ->
                service
                    .authByExchangeToken(
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
                    .flatMap {
                        if (it.error != null) {
                            Single.error(
                                AuthException(it.error.orEmpty(), it.errorDescription)
                            )
                        } else {
                            Single.just(it)
                        }
                    }
            }
    }

    companion object {
        fun <T : Any> extractResponseWithErrorHandling(): Function<BaseResponse<T>, T> {
            return Function { response: BaseResponse<T> ->
                response.error?.let { throw Exceptions.propagate(ApiException(it)) }
                    ?: (response.response
                        ?: throw NullPointerException("VK return null response"))
            }
        }
    }
}