package dev.ragnarok.fenrir.activity.captcha.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.activity.captcha.VK_CAPTCHA_CHALLENGE_DOMAIN_URL_KEY
import dev.ragnarok.fenrir.activity.captcha.VK_CAPTCHA_URL_KEY
import dev.ragnarok.fenrir.activity.captcha.di.DI.Companion.di
import dev.ragnarok.fenrir.activity.captcha.sensors.HandlerThreadProvider
import dev.ragnarok.fenrir.activity.captcha.sensors.model.SensorsData
import dev.ragnarok.fenrir.activity.captcha.sensors.model.toJson
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.coroutines.CancelableJob
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.delayTaskFlow
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toMain
import dev.ragnarok.fenrir.view.natives.animation.ThorVGLottieView
import org.json.JSONObject

internal val webViewProvider by lazy {
    HandlerThreadProvider("vk-webview-thread")
}

internal class VKCaptchaWebViewActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var mLoadingProgressBar: ThorVGLottieView

    private var mLoadingProgressBarDispose = CancelableJob()
    private var mLoadingProgressBarLoaded = false

    private val urlDecorator by lazy { UrlDecorator(resources.configuration) }
    private val webViewHandler = Handler(webViewProvider.provide().looper)

    private val domain by lazy { intent.getStringExtra(VK_CAPTCHA_CHALLENGE_DOMAIN_URL_KEY) }
    private val isHitmanChallenge by lazy { domain != null }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Utils.updateActivityContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setTranslucent(true)
        }
        window.setBackgroundDrawableResource(R.color.transparent)
        setContentView(R.layout.activity_vkcaptcha)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })

        actionBar?.hide()

        webView = findViewById(R.id.webview)
        mLoadingProgressBar = findViewById(R.id.loading_progress_bar)
        val url = intent.getStringExtra(VK_CAPTCHA_URL_KEY)!!
        setupWebView(url)
    }

    fun displayLoading(loading: Boolean) {
        mLoadingProgressBarDispose.cancel()
        if (loading) {
            mLoadingProgressBarDispose += delayTaskFlow(300).toMain {
                mLoadingProgressBarLoaded = true
                mLoadingProgressBar.visibility = View.VISIBLE
                mLoadingProgressBar.fromRes(
                    dev.ragnarok.fenrir_common.R.raw.loading,
                    intArrayOf(
                        0x000000,
                        Color.WHITE,
                        0x777777,
                        Color.WHITE
                    )
                )
                mLoadingProgressBar.startAnimation()
            }
        } else if (mLoadingProgressBarLoaded) {
            mLoadingProgressBarLoaded = false
            mLoadingProgressBar.visibility = View.GONE
            mLoadingProgressBar.clearAnimationDrawable(
                callSuper = true, clearState = true,
                cancelTask = true
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(url: String) {
        webView.apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(
                VKCaptchaJSInterface(
                    handler = webViewHandler,
                    onClose = { finishActivity() },
                    onDataUpdate = ::sendVKCaptchaListenSensorsChangedEvent,
                    sensorsDataRepository = di.sensorsDataRepository,
                    domain = domain
                ),
                VKCaptchaJSInterface.VK_CAPTCHA_JS_INTERFACE
            )
            setBackgroundColor(Color.TRANSPARENT)
            webViewClient = VKCaptchaWebViewClient(
                onError = { finishActivity() },
                onPageLoaded = {
                    displayLoading(false)
                    webView.visibility = View.VISIBLE
                },
                isHitmanChallenge = isHitmanChallenge,
                startUrl = url
            )
        }
        displayLoading(true)
        val newUrl = prepareUrl(url)
        webView.loadUrl(newUrl)
    }

    private fun sendVKCaptchaListenSensorsChangedEvent(data: SensorsData) {
        if (isFinishing) {
            return
        }
        val result = JSONObject().apply {
            put("detail", data.toJson())
        }
        webView.loadUrl("javascript:window.dispatchEvent(new CustomEvent('VKCaptchaListenSensorsChanged', ${result}))")
    }

    private fun prepareUrl(url: String): String {
        return urlDecorator.addScheme(url)
    }

    private fun finishActivity() {
        this.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.fragment_enter,
                R.anim.fragment_exit
            )
        } else {
            @Suppress("deprecation")
            overridePendingTransition(R.anim.fragment_enter, R.anim.fragment_exit)
        }
    }

    override fun onDestroy() {
        webView.removeJavascriptInterface(VKCaptchaJSInterface.VK_CAPTCHA_JS_INTERFACE)
        di.sensorsDataRepository.stopListening()
        webView.destroy()
        mLoadingProgressBarDispose.cancel()
        super.onDestroy()
    }
}