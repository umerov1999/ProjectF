package dev.ragnarok.fenrir.api.validation

import dev.ragnarok.fenrir.api.OutOfDateException

interface IVKIdCaptchaProvider {
    fun requestCaptcha(redirectUri: String, domain: String)
    fun cancel(redirectUri: String)

    @Throws(OutOfDateException::class)
    fun lookupSuccessToken(redirectUri: String): String?

    fun enterState(redirectUri: String, successToken: String?)
}