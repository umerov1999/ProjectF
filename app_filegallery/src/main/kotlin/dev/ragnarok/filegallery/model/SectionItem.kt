package dev.ragnarok.filegallery.model

import androidx.annotation.IntDef

@IntDef(
    SectionItem.NULL,
    SectionItem.FILE_MANAGER,
    SectionItem.SETTINGS,
    SectionItem.LOCAL_SERVER,
    SectionItem.TAGS
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class SectionItem {
    companion object {
        const val NULL = -1
        const val FILE_MANAGER = 0
        const val SETTINGS = 1
        const val LOCAL_SERVER = 2
        const val TAGS = 3
    }
}