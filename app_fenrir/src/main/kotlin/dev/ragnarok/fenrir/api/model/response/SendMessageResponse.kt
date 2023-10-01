package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.MessageError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SendMessageResponse {
    @SerialName("peer_id")
    var peer_id = 0L

    @SerialName("message_id")
    var message_id = 0

    @SerialName("conversation_message_id")
    var conversation_message_id = 0

    @SerialName("error")
    var error: MessageError? = null
}
