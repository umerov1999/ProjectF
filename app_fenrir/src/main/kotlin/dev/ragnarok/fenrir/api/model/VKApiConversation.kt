package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiConversation {
    @SerialName("peer")
    var peer: Peer? = null

    /**
     * идентификатор последнего прочтенного входящего сообщения.
     */
    @SerialName("in_read")
    var inRead = 0

    /**
     * идентификатор последнего прочтенного исходящего сообщения.
     */
    @SerialName("out_read")
    var outRead = 0

    @SerialName("last_message_id")
    var lastMessageId = 0

    /**
     * число непрочитанных сообщений.
     */
    @SerialName("unread_count")
    var unreadCount = 0

    @SerialName("sort_id")
    var sort_id: SortElement? = null

    /**
     * true, если диалог помечен как важный (только для сообщений сообществ).
     */
    @SerialName("important")
    var important = false

    @SerialName("current_keyboard")
    var current_keyboard: CurrentKeyboard? = null

    /**
     * true, если диалог помечен как неотвеченный (только для сообщений сообществ).
     */
    @SerialName("unanswered")
    var unanswered = false

    /**
     * информация о том, может ли пользователь писать в диалог.
     */
    @SerialName("can_write")
    var canWrite: CanWrite? = null

    @SerialName("chat_settings")
    var settings: Settings? = null

    @Serializable
    class CanWrite {
        /**
         * true, если пользователь может писать в диалог;
         */
        @SerialName("allowed")
        var allowed = false

        /**
         * 18 — пользователь заблокирован или удален;
         * 900 — нельзя отправить сообщение пользователю, который в чёрном списке;
         * 901 — пользователь запретил сообщения от сообщества;
         * 902 — пользователь запретил присылать ему сообщения с помощью настроек приватности;
         * 915 — в сообществе отключены сообщения;
         * 916 — в сообществе заблокированы сообщения;
         * 917 — нет доступа к чату;
         * 918 — нет доступа к e-mail;
         * 203 — нет доступа к сообществу.
         */
        @SerialName("reason")
        var reason = 0
    }

    @Serializable
    class Peer {
        /**
         * идентификатор назначения.
         */
        @SerialName("id")
        var id = 0L

        /**
         * local_id (integer) — локальный идентификатор назначения. Для чатов — id - 2000000000, для сообществ — -id, для e-mail — -(id+2000000000).
         * для контактов - (id-1900000000)
         */
        @SerialName("local_id")
        var local_id = 0L

        @SerialName("type")
        var type: String? = null
    }

    @Serializable
    class Settings {
        @SerialName("pinned_message")
        var pinnedMesage: VKApiMessage? = null

        @SerialName("title")
        var title: String? = null

        @SerialName("members_count")
        var membersCount = 0

        @SerialName("photo")
        var photo: Photo? = null

        @SerialName("active_ids")
        var activeIds: LongArray? = null

        @SerialName("state")
        var state: String? = null

        @SerialName("is_group_channel")
        var is_group_channel = false

        @SerialName("acl")
        var acl: Acl? = null
    }

    @Serializable
    class Acl {
        @SerialName("can_invite")
        var can_invite = false

        @SerialName("can_change_info")
        var can_change_info = false

        @SerialName("can_change_pin")
        var can_change_pin = false

        @SerialName("can_promote_users")
        var can_promote_users = false

        @SerialName("can_see_invite_link")
        var can_see_invite_link = false

        @SerialName("can_change_invite_link")
        var can_change_invite_link = false
    }

    @Serializable
    class Photo {
        @SerialName("photo_50")
        var photo50: String? = null

        @SerialName("photo_100")
        var photo100: String? = null

        @SerialName("photo200")
        var photo200: String? = null
    }

    @Serializable
    class Keyboard_Action {
        @SerialName("type")
        var type: String? = null

        @SerialName("label")
        var label: String? = null

        @SerialName("link")
        var link: String? = null

        @SerialName("payload")
        var payload: String? = null
    }

    @Serializable
    class ButtonElement {
        @SerialName("action")
        var action: Keyboard_Action? = null

        @SerialName("color")
        var color: String? = null
    }

    @Serializable
    class SortElement {
        @SerialName("major_id")
        var major_id = 0

        @SerialName("minor_id")
        var minor_id = 0
    }

    @Serializable
    class ContactElement {
        @SerialName("id")
        var id = 0L

        @SerialName("name")
        var name: String? = null

        @SerialName("phone")
        var phone: String? = null

        @SerialName("photo_50")
        var photo_50: String? = null

        @SerialName("photo_100")
        var photo_100: String? = null

        @SerialName("photo_200")
        var photo_200: String? = null

        @SerialName("photo_max_orig")
        var photo_max_orig: String? = null

        @SerialName("last_seen_status")
        var last_seen_status: String? = null
    }

    @Serializable
    class CurrentKeyboard {
        @SerialName("one_time")
        var one_time = false

        @SerialName("inline")
        var inline = false

        @SerialName("author_id")
        var author_id = 0L

        @SerialName("buttons")
        var buttons: List<List<ButtonElement>>? = null
    }
}