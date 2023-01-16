package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ConversationDeleteResult {
    @SerialName("last_deleted_id")
    var lastDeletedId = 0L
}