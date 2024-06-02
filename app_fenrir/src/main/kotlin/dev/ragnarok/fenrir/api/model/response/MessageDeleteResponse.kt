package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MessageDeleteResponse {
    @SerialName("conversation_message_id")
    var conversation_message_id: Int = 0

    @SerialName("message_id")
    var message_id: Int = 0

    @SerialName("peer_id")
    var peer_id: Long = 0L

    @SerialName("response")
    var response: Int = 0
}
