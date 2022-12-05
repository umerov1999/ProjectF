package dev.ragnarok.fenrir.upload

import androidx.annotation.IntDef

@IntDef(MessageMethod.NULL, MessageMethod.PHOTO, MessageMethod.VIDEO, MessageMethod.AUDIO)
@Retention(AnnotationRetention.SOURCE)
annotation class MessageMethod {
    companion object {
        const val NULL = 1
        const val PHOTO = 2
        const val VIDEO = 3
        const val AUDIO = 4
    }
}