package dev.ragnarok.fenrir.service

object ApiErrorCodes {
    const val TOO_MANY_REQUESTS_PER_SECOND = 6
    const val USER_AUTHORIZATION_FAILED = 5
    const val CAPTCHA_NEED = 14
    const val ACCESS_DENIED = 15
    const val VALIDATE_NEED = 17
    const val REFRESH_TOKEN = 25
    const val PAGE_HAS_BEEN_REMOVED_OR_BLOCKED = 18
    const val CLIENT_VERSION_DEPRECATED = 34
}