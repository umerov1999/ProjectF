package dev.ragnarok.fenrir.model

import androidx.annotation.IntDef

@IntDef(
    MessageFlag.UNREAD,
    MessageFlag.OUTBOX,
    MessageFlag.REPLIED,
    MessageFlag.IMPORTANT,
    MessageFlag.DIALOG,
    MessageFlag.FRIENDS,
    MessageFlag.SPAM,
    MessageFlag.DELETED,
    MessageFlag.FIXED,
    MessageFlag.MEDIA,
    MessageFlag.GROUP_CHAT
)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class MessageFlag {
    companion object {
        const val UNREAD = 1 //сообщение не прочитано
        const val OUTBOX = 2 //исходящее сообщение
        const val REPLIED = 4 //на сообщение был создан ответ
        const val IMPORTANT = 8 //помеченное сообщение
        const val DIALOG = 16 //сообщение отправлено через диалог
        const val FRIENDS = 32 //сообщение отправлено другом
        const val SPAM = 64 //сообщение помечено как "Спам"
        const val DELETED = 128 //сообщение удалено (в корзине)
        const val FIXED = 256 //сообщение проверено пользователем на спам
        const val MEDIA = 512 //сообщение содержит медиаконтент
        const val GROUP_CHAT = 8192 //беседа
    }
}