package dev.ragnarok.fenrir.activity.captcha.web

import android.content.res.Configuration
import androidx.core.net.toUri

internal class UrlDecorator(
    private val configuration: Configuration
) {

    fun addScheme(url: String): String {
        return url.toUri()
            .buildUpon()
            .appendQueryParameter("scheme", getTheme())
            .build()
            .toString()
    }

    private fun getTheme(): String {
        val currentNightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                "light"
            }

            Configuration.UI_MODE_NIGHT_YES -> {
                "dark"
            }

            else -> {
                "light"
            }
        }
    }
}