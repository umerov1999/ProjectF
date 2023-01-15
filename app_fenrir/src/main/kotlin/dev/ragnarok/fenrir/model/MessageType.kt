package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    MessageType.NO,
    MessageType.STICKER,
    MessageType.GRAFFITI,
    MessageType.CALL,
    MessageType.GIFT,
    MessageType.VOICE,
    MessageType.VIDEO,
    MessageType.AUDIO,
    MessageType.DOC,
    MessageType.PHOTO,
    MessageType.WALL,
    MessageType.OTHERS
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class MessageType {
    companion object {
        const val NO = 0
        const val STICKER = 1
        const val GRAFFITI = 2
        const val CALL = 3
        const val GIFT = 4
        const val VOICE = 5
        const val VIDEO = 6
        const val AUDIO = 7
        const val DOC = 8
        const val PHOTO = 9
        const val WALL = 10
        const val OTHERS = 11
    }
}