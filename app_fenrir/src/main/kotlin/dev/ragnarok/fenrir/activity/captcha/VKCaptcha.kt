package dev.ragnarok.fenrir.activity.captcha

import android.content.Intent
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.activity.captcha.VKCaptcha.passChallenge
import dev.ragnarok.fenrir.activity.captcha.challenge.CaptchaStorage
import dev.ragnarok.fenrir.activity.captcha.challenge.VKChallengeResultListener
import dev.ragnarok.fenrir.activity.captcha.di.DI
import dev.ragnarok.fenrir.activity.captcha.di.DI.Companion.di
import dev.ragnarok.fenrir.activity.captcha.web.VKCaptchaWebViewActivity

internal var result: VKCaptchaState? = null
    set(value) {
        when (value) {
            is VKCaptchaState.Success -> {
                val result = value.result
                VKCaptcha.captchaListener?.onResult(result)
                if (result.domain != null && result.token != null) {
                    VKCaptcha.captchaStorage.addToken(result.domain, result.token)
                }
                VKCaptcha.challengeListener?.onResult(result)
            }

            is VKCaptchaState.Error -> {
                val result = VKCaptchaResult(
                    token = null,
                    error = value.error
                )
                VKCaptcha.captchaListener?.onResult(result)
                VKCaptcha.challengeListener?.onResult(result)
            }

            is VKCaptchaState.CancelledByClient -> {
                VKCaptcha.captchaListener?.onUserCancel()
                VKCaptcha.challengeListener?.onUserCancel()
            }

            else -> {}
        }
        field = value
    }

internal const val VK_CAPTCHA_URL_KEY = "VK_CAPTCHA_URL_KEY"
internal const val VK_CAPTCHA_CHALLENGE_DOMAIN_URL_KEY = "VK_CAPTCHA_CHALLENGE_DOMAIN_URL_KEY"

object VKCaptcha {

    internal var captchaListener: VKCaptchaResultListener? = null

    internal var challengeListener: VKChallengeResultListener? = null
    internal val captchaStorage: CaptchaStorage by lazy { CaptchaStorage() }

    // TODO: FORT-931 Check is background
    fun openCaptcha(
        domain: String? = null,
        redirectUri: String,
        listener: VKCaptchaResultListener
    ) {
        val appContext = Includes.provideApplicationContext()
        DI.init(appContext)
        captchaListener = listener
        if (!isInternetAvailable()) {
            val networkError = VKCaptchaError.NetworkError("There is no internet connection")
            closeCaptcha(VKCaptchaState.Error(networkError))
            return
        }
        val intent = Intent(appContext, VKCaptchaWebViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(VK_CAPTCHA_URL_KEY, redirectUri)
        intent.putExtra(VK_CAPTCHA_CHALLENGE_DOMAIN_URL_KEY, domain)
        appContext.startActivity(intent)
    }

    fun closeCaptcha() {
        closeCaptcha(VKCaptchaState.CancelledByClient)
    }

    internal fun closeCaptcha(state: VKCaptchaState) {
        di.sensorsDataRepository.stopListening()
        result = state
    }

    /**
     * Метод прохождения Hitman Challenge для получения токена X-Challenge-Solution
     * @param domain domain запроса, для которого необходимо пройти Hitman Challenge
     * @param challengeUrl значение X-Challenge-Url
     */
    fun passChallenge(
        domain: String,
        challengeUrl: String,
        listener: VKChallengeResultListener
    ) {
        challengeListener = listener
        if (!isChallengeUrl(challengeUrl)) {
            val error =
                VKCaptchaError.IllegalArgumentError("challengeUrl is not match with Hitman Challenge url")
            listener.onResult(
                VKCaptchaResult(
                    token = null,
                    error = error,
                    domain = domain
                )
            )
            return
        }
        val url = domain + challengeUrl
        openCaptcha(
            domain = domain,
            redirectUri = url,
            listener = object : VKCaptchaResultListener {
                override fun onResult(result: VKCaptchaResult) {}
                override fun onUserCancel() {}
            }
        )
    }

    /**
     * Метод получения токена X-Challenge-Solution после пройденного Hitman Challenge
     * @see passChallenge
     * @return Токен X-Challenge-Solution
     */
    fun getHitmanToken(domain: String): String? = captchaStorage.getToken(domain)

    internal fun setResult(captchaResult: VKCaptchaResult) {
        result = VKCaptchaState.Success(captchaResult)
    }

    private fun isInternetAvailable(): Boolean = di.networkConnectionObserver.isInternetAvailable()

    private fun isChallengeUrl(url: String): Boolean = url.endsWith("/challenge.html")
}
