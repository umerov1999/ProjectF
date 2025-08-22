package dev.ragnarok.fenrir.activity.captcha

interface VKCaptchaResultListener {
    fun onResult(result: VKCaptchaResult)
    fun onUserCancel()
}
