package dev.ragnarok.fenrir.activity.captcha.web

import android.content.Intent
import android.net.Uri
import android.view.View

internal class UrlValidator(private val isHitmanChallenge: Boolean, private val startUrl: String) {

    /**
     * @return `true` to cancel the current load, otherwise return `false`.
     * @see android.webkit.WebViewClient.shouldOverrideUrlLoading
     */
    fun shouldOverrideUrlLoading(view: View?, url: Uri?): Boolean {
        when {
            // внутри hitman-капчи происходит редирект по 302 коду, где проставляются параметры
            url != null && isHitmanChallenge && url.toString().startsWith(startUrl) -> {
                return false
            }
            // переход внутри VK
            url != null && view != null && LinkScheme().isVkLink(url.toString()) -> {
                val intent = Intent(Intent.ACTION_VIEW).setData(url)
                view.context.startActivity(intent)
                return true
            }
            // Все другие url игнорируются
            else -> {
                return true
            }
        }
    }

}