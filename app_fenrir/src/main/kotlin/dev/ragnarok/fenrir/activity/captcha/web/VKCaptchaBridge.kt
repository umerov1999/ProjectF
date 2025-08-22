package dev.ragnarok.fenrir.activity.captcha.web

import android.webkit.JavascriptInterface

@Suppress("FunctionName")
internal interface VKCaptchaBridge {
    @JavascriptInterface
    fun VKCaptchaCloseCaptcha(data: String)

    @JavascriptInterface
    fun VKCaptchaGetResult(data: String)

    @JavascriptInterface
    fun VKCaptchaListenSensorsStart(data: String)

    @JavascriptInterface
    fun VKCaptchaListenSensorsStop(data: String)
}
