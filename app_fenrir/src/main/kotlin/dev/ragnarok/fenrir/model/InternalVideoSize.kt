package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    InternalVideoSize.SIZE_240,
    InternalVideoSize.SIZE_360,
    InternalVideoSize.SIZE_480,
    InternalVideoSize.SIZE_720,
    InternalVideoSize.SIZE_1080,
    InternalVideoSize.SIZE_1440,
    InternalVideoSize.SIZE_2160,
    InternalVideoSize.SIZE_HLS,
    InternalVideoSize.SIZE_LIVE
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class InternalVideoSize {
    companion object {
        const val SIZE_240 = 240
        const val SIZE_360 = 360
        const val SIZE_480 = 480
        const val SIZE_720 = 720
        const val SIZE_1080 = 1080
        const val SIZE_1440 = 1440
        const val SIZE_2160 = 2160
        const val SIZE_HLS = -1
        const val SIZE_LIVE = -2
    }
}