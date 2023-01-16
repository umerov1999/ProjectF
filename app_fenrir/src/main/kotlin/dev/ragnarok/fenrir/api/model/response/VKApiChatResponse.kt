package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiChatResponse {
    @SerialName("chat_id")
    var chat_id = 0L
}