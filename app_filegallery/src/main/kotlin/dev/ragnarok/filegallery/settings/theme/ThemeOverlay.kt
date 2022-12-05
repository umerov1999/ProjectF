package dev.ragnarok.filegallery.settings.theme

import androidx.annotation.IntDef

@IntDef(ThemeOverlay.OFF, ThemeOverlay.AMOLED, ThemeOverlay.MD1)
@Retention(AnnotationRetention.SOURCE)
annotation class ThemeOverlay {
    companion object {
        const val OFF = 0
        const val AMOLED = 1
        const val MD1 = 2
    }
}