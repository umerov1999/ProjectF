package dev.ragnarok.fenrir.crypt

import androidx.annotation.IntDef

@IntDef(MessageType.KEY_EXCHANGE, MessageType.CRYPTED, MessageType.NORMAL)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class MessageType {
    companion object {
        const val KEY_EXCHANGE = 1
        const val CRYPTED = 2
        const val NORMAL = 0
    }
}