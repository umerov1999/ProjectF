package dev.ragnarok.fenrir.settings

import androidx.annotation.IntDef

@IntDef(AvatarStyle.CIRCLE, AvatarStyle.OVAL)
@Retention(AnnotationRetention.SOURCE)
annotation class AvatarStyle {
    companion object {
        const val CIRCLE = 1
        const val OVAL = 2
    }
}