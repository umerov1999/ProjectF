package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    DocType.TEXT,
    DocType.ARCHIVE,
    DocType.GIF,
    DocType.IMAGE,
    DocType.AUDIO,
    DocType.VIDEO,
    DocType.EBOOK,
    DocType.UNKNOWN
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class DocType {
    companion object {
        const val TEXT = 1
        const val ARCHIVE = 2
        const val GIF = 3
        const val IMAGE = 4
        const val AUDIO = 5
        const val VIDEO = 6
        const val EBOOK = 7
        const val UNKNOWN = 8
    }
}