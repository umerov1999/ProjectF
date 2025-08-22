package dev.ragnarok.fenrir.activity.captcha

data class VKCaptchaResult(
    val token: String?,
    val error: VKCaptchaError?,
    val domain: String? = null,
)
