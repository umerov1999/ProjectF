package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    ChatAction.NO_ACTION,
    ChatAction.PHOTO_UPDATE,
    ChatAction.PHOTO_REMOVE,
    ChatAction.CREATE,
    ChatAction.TITLE_UPDATE,
    ChatAction.INVITE_USER,
    ChatAction.KICK_USER,
    ChatAction.PIN_MESSAGE,
    ChatAction.UNPIN_MESSAGE,
    ChatAction.INVITE_USER_BY_LINK
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class ChatAction {
    companion object {
        const val NO_ACTION = 0
        const val PHOTO_UPDATE = 1
        const val PHOTO_REMOVE = 2
        const val CREATE = 3
        const val TITLE_UPDATE = 4
        const val INVITE_USER = 5
        const val KICK_USER = 6
        const val PIN_MESSAGE = 7
        const val UNPIN_MESSAGE = 8
        const val INVITE_USER_BY_LINK = 9
    }
}