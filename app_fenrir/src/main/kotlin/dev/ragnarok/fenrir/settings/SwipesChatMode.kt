package dev.ragnarok.fenrir.settings

import androidx.annotation.IntDef

@IntDef(SwipesChatMode.DISABLED, SwipesChatMode.SLIDR)
@Retention(AnnotationRetention.SOURCE)
annotation class SwipesChatMode {
    companion object {
        const val DISABLED = 0
        const val SLIDR = 1
    }
}