package dev.ragnarok.fenrir.util

import androidx.annotation.IntDef

@IntDef(
    MainActivityTransforms.MAIN,
    MainActivityTransforms.SWIPEBLE,
    MainActivityTransforms.SEND_ATTACHMENTS,
    MainActivityTransforms.PROFILES
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class MainActivityTransforms {
    companion object {
        const val MAIN = 1
        const val SWIPEBLE = 2
        const val SEND_ATTACHMENTS = 3
        const val PROFILES = 4
    }
}