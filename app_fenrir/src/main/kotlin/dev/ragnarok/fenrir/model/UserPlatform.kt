package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    UserPlatform.MOBILE,
    UserPlatform.IPHONE,
    UserPlatform.IPAD,
    UserPlatform.ANDROID,
    UserPlatform.WPHONE,
    UserPlatform.WINDOWS,
    UserPlatform.WEB,
    UserPlatform.UNKNOWN
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class UserPlatform {
    companion object {
        const val UNKNOWN = 0
        const val MOBILE = 1
        const val IPHONE = 2
        const val IPAD = 3
        const val ANDROID = 4
        const val WPHONE = 5
        const val WINDOWS = 6
        const val WEB = 7
    }
}