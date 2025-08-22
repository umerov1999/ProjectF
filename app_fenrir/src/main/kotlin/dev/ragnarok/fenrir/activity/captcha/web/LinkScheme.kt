package dev.ragnarok.fenrir.activity.captcha.web

import android.net.Uri
import android.webkit.URLUtil
import androidx.core.net.toUri
import java.util.Locale

internal class LinkScheme {

    private val hostRegex by lazy {
        "(^|[a-z0-9.\\-]*\\.)(vk|vkontakte)\\.(com|ru|me)".toRegex()
    }

    fun isVkLink(url: String): Boolean {
        return if (URLUtil.isHttpsUrl(url)) {
            val uri = url.toUri()
            isVKHost(uri)
        } else {
            false
        }
    }

    private fun isVKHost(uri: Uri): Boolean {
        if (uri.host.isNullOrEmpty()) return false
        val host = uri.host.toString().toLowerCaseDefault()
        return hostRegex.matches(host)
    }

    private fun String.toLowerCaseDefault() = lowercase(Locale.getDefault())
}