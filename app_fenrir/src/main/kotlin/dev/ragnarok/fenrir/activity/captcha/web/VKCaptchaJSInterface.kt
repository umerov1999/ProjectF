package dev.ragnarok.fenrir.activity.captcha.web

import android.os.Handler
import android.util.Log
import android.webkit.JavascriptInterface
import dev.ragnarok.fenrir.activity.captcha.VKCaptcha
import dev.ragnarok.fenrir.activity.captcha.VKCaptchaResult
import dev.ragnarok.fenrir.activity.captcha.sensors.SensorsDataRepository
import dev.ragnarok.fenrir.activity.captcha.sensors.model.PeriodMs
import dev.ragnarok.fenrir.activity.captcha.sensors.model.SensorsData
import dev.ragnarok.fenrir.activity.captcha.sensors.model.toListOfSensors
import org.json.JSONException
import org.json.JSONObject

internal class VKCaptchaJSInterface(
    private val handler: Handler,
    private var onClose: () -> Unit,
    private val onDataUpdate: (SensorsData) -> Unit,
    private val sensorsDataRepository: SensorsDataRepository,
    private val domain: String?
) : VKCaptchaBridge {

    private var isClosedByUser = true

    @JavascriptInterface
    override fun VKCaptchaCloseCaptcha(data: String) {
        if (isClosedByUser) {
            VKCaptcha.closeCaptcha()
        }
        onClose.invoke()
    }

    @JavascriptInterface
    override fun VKCaptchaGetResult(data: String) {
        try {
            val json = JSONObject(data)
            val result = VKCaptchaResult(
                token = json.getString("token"),
                error = null,
                //  TODO добавить каст ошибок с веба в ошибки андроида и придумать эти ошибки
                domain = domain,
            )
            isClosedByUser = false
            handler.post { VKCaptcha.setResult(result) }
            sensorsDataRepository.stopListening()
        } catch (e: JSONException) {
            Log.e(VK_CAPTCHA_WEB_VIEW, "Error when parsing json\n Error:$e")
        }
    }

    @JavascriptInterface
    override fun VKCaptchaListenSensorsStart(data: String) {
        try {
            val json = JSONObject(data)
            val periodMs = json.optInt("period", -1)
            val sensors = json.getJSONArray("bridge_sensors_list")
            if (periodMs == -1) {
                throw IllegalStateException("No period value was provided from WebView")
            }
            sensorsDataRepository.startListening(
                sensors.toListOfSensors(),
                PeriodMs(periodMs),
                onDataUpdate = {
                    onDataUpdate.invoke(it)
                })
        } catch (e: JSONException) {
            Log.e(VK_CAPTCHA_WEB_VIEW, "Error when parsing json\n Error:$e")
        }
    }

    @JavascriptInterface
    override fun VKCaptchaListenSensorsStop(data: String) {
        sensorsDataRepository.stopListening()
    }

    companion object {
        const val VK_CAPTCHA_JS_INTERFACE = "AndroidBridge"
        const val VK_CAPTCHA_WEB_VIEW = "VKCaptchaWebView"
    }
}
