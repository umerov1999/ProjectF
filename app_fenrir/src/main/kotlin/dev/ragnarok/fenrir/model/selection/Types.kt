package dev.ragnarok.fenrir.model.selection

import androidx.annotation.IntDef

@IntDef(Types.LOCAL_PHOTOS, Types.VK_PHOTOS, Types.FILES, Types.VIDEOS, Types.LOCAL_GALLERY)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class Types {
    companion object {
        const val LOCAL_PHOTOS = 0
        const val VK_PHOTOS = 1
        const val FILES = 2
        const val VIDEOS = 3
        const val LOCAL_GALLERY = 4
    }
}