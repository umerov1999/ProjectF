package dev.ragnarok.fenrir.activity.captcha.web

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.net.toUri
import dev.ragnarok.fenrir.activity.captcha.VKCaptcha
import dev.ragnarok.fenrir.activity.captcha.VKCaptchaError
import dev.ragnarok.fenrir.activity.captcha.VKCaptchaState

internal class VKCaptchaWebViewClient(
    private val onError: () -> Unit,
    private val onPageLoaded: () -> Unit,
    private val isHitmanChallenge: Boolean = false,
    private val startUrl: String,
) : WebViewClient() {

    private val urlValidator by lazy(LazyThreadSafetyMode.NONE) {
        UrlValidator(isHitmanChallenge, startUrl)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        onPageLoaded.invoke()
        super.onPageStarted(view, url, favicon)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        view?.loadUrl(HTML_PATH)
        closeWebView("Error loading WebView.")
        super.onReceivedError(view, request, error)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        if (startUrl.toUri().host == request?.url?.host) {
            closeWebView("HttpError loading WebView. ErrorCode: ${errorResponse?.statusCode}")
        }
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        urlValidator.shouldOverrideUrlLoading(view, request?.url)

    private fun closeWebView(message: String) {
        VKCaptcha.closeCaptcha(state = VKCaptchaState.Error(VKCaptchaError.NetworkError(message)))
        onError.invoke()
    }

    private companion object {
        private const val HTML_PATH = "file:///android_asset/index.html"
    }
}
