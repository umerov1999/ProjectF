package dev.ragnarok.fenrir.util

import android.annotation.SuppressLint
import androidx.core.util.PatternsCompat
import dev.ragnarok.fenrir.trimmedIsNullOrEmpty

object ValidationUtil {

    @SuppressLint("RestrictedApi")
    fun isValidURL(url: String?): Boolean {
        return url != null && PatternsCompat.AUTOLINK_WEB_URL.matcher(url).find()
    }


    fun isValidIpAddress(ipv4: String?): Boolean {
        ipv4 ?: return false
        var ipv4T = ipv4
        if (ipv4.trimmedIsNullOrEmpty()) {
            return false
        }
        ipv4T = ipv4T.trim { it <= ' ' }
        val blocks = ipv4T.split(Regex("\\.")).toTypedArray()
        if (blocks.size != 4) {
            return false
        }
        for (block in blocks) {
            try {
                val num = block.toInt()
                if (num > 255 || num < 0) {
                    return false
                }
            } catch (e: Exception) {
                return false
            }
        }
        return true
    }
}