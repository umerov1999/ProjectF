package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiBanned {
    @SerialName("type")
    var type: String? = null

    @SerialName("profile")
    var profile: VKApiUser? = null

    @SerialName("group")
    var group: VKApiCommunity? = null

    @SerialName("ban_info")
    var banInfo: Info? = null

    @Serializable
    class Info {
        /**
         * идентификатор администратора, который добавил пользователя в черный список.
         */
        @SerialName("admin_id")
        var adminId = 0

        /**
         * дата добавления пользователя в черный список в формате Unixtime.
         */
        @SerialName("date")
        var date: Long = 0

        /**
         * причина добавления пользователя в черный список. Возможные значения:
         * 0 — другое (по умолчанию);
         * 1 — спам;
         * 2 — оскорбление участников;
         * 3 — нецензурные выражения;
         * 4 — сообщения не по теме.
         */
        @SerialName("reason")
        var reason = 0

        /**
         * текст комментария.
         */
        @SerialName("comment")
        var comment: String? = null

        /**
         * дата окончания блокировки (0 — блокировка вечная).
         */
        @SerialName("end_date")
        var endDate: Long = 0

        @SerialName("comment_visible")
        var commentVisible = false
    }
}