package dev.ragnarok.fenrir.activity.captcha

internal sealed class VKCaptchaState {
    class Success(val result: VKCaptchaResult) : VKCaptchaState()
    data object CancelledByClient : VKCaptchaState()
    class Error(val error: VKCaptchaError) : VKCaptchaState()
}

sealed class VKCaptchaError(
    val message: String,
    val error: Throwable?
) {
    class NetworkError(message: String, error: Throwable? = null) : VKCaptchaError(message, error)
    class IllegalArgumentError(message: String) :
        VKCaptchaError(message, IllegalArgumentException(message))
}
