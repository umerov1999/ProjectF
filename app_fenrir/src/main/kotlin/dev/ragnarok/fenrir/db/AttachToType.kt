package dev.ragnarok.fenrir.db

import androidx.annotation.IntDef

@IntDef(AttachToType.MESSAGE, AttachToType.COMMENT, AttachToType.POST)
@Retention(AnnotationRetention.SOURCE)
annotation class AttachToType {
    companion object {
        const val MESSAGE = 1
        const val COMMENT = 2
        const val POST = 3
    }
}