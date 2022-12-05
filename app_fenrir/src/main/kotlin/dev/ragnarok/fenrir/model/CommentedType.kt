package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(CommentedType.POST, CommentedType.PHOTO, CommentedType.VIDEO, CommentedType.TOPIC)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class CommentedType {
    companion object {
        const val POST = 1
        const val PHOTO = 2
        const val VIDEO = 3
        const val TOPIC = 4
    }
}