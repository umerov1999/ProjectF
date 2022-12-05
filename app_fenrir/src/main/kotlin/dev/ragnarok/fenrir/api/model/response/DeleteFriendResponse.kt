package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.Serializable

@Serializable
class DeleteFriendResponse {
    /**
     * удалось успешно удалить друга
     */
    var success = false

    /**
     * был удален друг
     */
    var friend_deleted = false

    /**
     * отменена исходящая заявка
     */
    var out_request_deleted = false

    /**
     * отклонена входящая заявка
     */
    var in_request_deleted = false

    /**
     * отклонена рекомендация друга
     */
    var suggestion_deleted = false
}