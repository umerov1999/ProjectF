package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    MessageStatus.SENT,
    MessageStatus.SENDING,
    MessageStatus.QUEUE,
    MessageStatus.ERROR,
    MessageStatus.EDITING,
    MessageStatus.WAITING_FOR_UPLOAD
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class MessageStatus {
    companion object {
        const val SENT = 1
        const val SENDING = 2
        const val QUEUE = 3
        const val ERROR = 4
        const val EDITING = 6
        const val WAITING_FOR_UPLOAD = 7
    }
}