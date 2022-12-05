package dev.ragnarok.filegallery.model

import androidx.annotation.IntDef

@IntDef(FileType.error, FileType.folder, FileType.photo, FileType.video, FileType.audio)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class FileType {
    companion object {
        const val error = -1
        const val folder = 0
        const val photo = 1
        const val video = 2
        const val audio = 3
    }
}