package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.model.LoginResponse
import dev.ragnarok.fenrir.api.model.VKApiValidationResponse
import io.reactivex.rxjava3.core.Single

interface IAuthApi {
    fun directLogin(
        grantType: String?, clientId: Int, clientSecret: String?,
        username: String?, pass: String?, v: String?, twoFaSupported: Boolean,
        scope: String?, code: String?, captchaSid: String?, captchaKey: String?, forceSms: Boolean
    ): Single<LoginResponse>

    fun validatePhone(
        apiId: Int,
        clientId: Int,
        clientSecret: String?,
        sid: String?,
        v: String?
    ): Single<VKApiValidationResponse>
}