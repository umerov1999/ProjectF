package dev.ragnarok.fenrir.view

import androidx.annotation.DrawableRes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.model.VideoPlatform

object VideoServiceIcons {

    @DrawableRes
    fun getIconByType(platform: String?): Int? {
        return if (platform == null) {
            null
        } else when (platform) {
            VideoPlatform.COUB -> R.drawable.ic_coub
            VideoPlatform.VIMEO -> R.drawable.ic_vimeo
            VideoPlatform.YOUTUBE -> R.drawable.ic_youtube
            VideoPlatform.RUTUBE -> R.drawable.ic_rutube
            else -> null
        }
    }
}