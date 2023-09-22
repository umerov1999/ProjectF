package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.MessageDtoAdapter
import dev.ragnarok.fenrir.api.model.VKApiConversation.CurrentKeyboard
import kotlinx.serialization.Serializable

/**
 * A message object describes a private message
 */
@Serializable(with = MessageDtoAdapter::class)
class VKApiMessage
/**
 * Creates empty Country instance.
 */
{
    /**
     * Message ID. (Not returned for forwarded messages), positive number
     */
    var id = 0

    /**
     * For an incoming message, the user ID of the author. For an outgoing message, the user ID of the receiver.
     */
    var peer_id = 0L

    /**
     * For an incoming message, the user ID of the author. For an outgoing message, the user ID of the receiver.
     */
    var from_id = 0L

    /**
     * Date (in Unix time) when the message was sent.
     */
    var date: Long = 0

    /**
     * Message type (false — received, true — sent). (Not returned for forwarded messages.)
     */
    var out = false

    /**
     * Body of the message.
     */
    var body: String? = null

    /**
     * List of media-attachments;
     */
    var attachments: VKApiAttachments? = null

    /**
     * Array of forwarded messages (if any).
     */
    var fwd_messages: ArrayList<VKApiMessage>? = null

    /**
     * Whether the message is deleted (false — no, true — yes).
     */
    var important = false

    /**
     * Whether the message voice played (false — no, true — yes).
     */
    var was_listened = false

    /**
     * Whether the message is deleted (false — no, true — yes).
     */
    var deleted = false
    var keyboard: CurrentKeyboard? = null

    /**
     * поле передано, если это служебное сообщение
     * строка, может быть chat_photo_update или chat_photo_remove,
     * а с версии 5.14 еще и chat_create, chat_title_update, chat_invite_user, chat_kick_user
     */
    var action: String? = null

    /**
     * идентификатор пользователя (если > 0) или email (если < 0), которого пригласили или исключили
     * число, для служебных сообщений с action равным chat_invite_user или chat_kick_user
     */
    var action_mid = 0L

    /**
     * email, который пригласили или исключили
     * строка, для служебных сообщений с action равным chat_invite_user или chat_kick_user и отрицательным action_mid
     */
    var action_email: String? = null

    /**
     * изображение-обложка чата
     */
    var action_photo_50: String? = null
    var action_photo_100: String? = null
    var action_photo_200: String? = null

    /**
     * название беседы
     * строка, для служебных сообщений с action равным chat_create или chat_title_update
     */
    var action_text: String? = null

    /**
     * идентификатор, используемый при отправке сообщения. Возвращается только для исходящих сообщений.
     */
    var random_id: String? = null

    /**
     * is edited?
     */
    var payload // "payload":"null"
            : String? = null
    var conversation_message_id = 0
    var reactions: ArrayList<VKApiReaction>? = null
    var reaction_id: Int = 0
    var update_time: Long = 0

    companion object {
        const val CHAT_PEER = 2000000000
        const val CONTACT_PEER = 1900000000
        const val FLAG_UNREAD = 1 //сообщение не прочитано
        const val FLAG_OUTBOX = 2 //исходящее сообщение
        const val FLAG_REPLIED = 4 //на сообщение был создан ответ
        const val FLAG_IMPORTANT = 8 //помеченное сообщение

        /**
         * Message status (false — not read, true — read). (Not returned for forwarded messages.)
         */
        //public boolean read_state;
        const val FLAG_DIALOG = 16 //сообщение отправлено через диалог

        /**
         * Title of message or chat.
         */
        //public String title;
        const val FLAG_FRIENDS = 32 //сообщение отправлено другом
        const val FLAG_SPAM = 64 //сообщение помечено как "Спам"
        const val FLAG_DELETED = 128 //сообщение удалено (в корзине)
        const val FLAG_FIXED = 256 //сообщение проверено пользователем на спам
        const val FLAG_MEDIA = 512 //сообщение содержит медиаконтент
        const val FLAG_GROUP_CHAT = 8192 //беседа
        const val FLAG_DELETED_FOR_ALL = 131072 //флаг для сообщений, удаленных для получателей
    }
}