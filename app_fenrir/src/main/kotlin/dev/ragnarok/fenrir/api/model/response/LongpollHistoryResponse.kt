package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LongpollHistoryResponse {
    @SerialName("messages")
    var messages: Messages? = null

    @Serializable
    class Messages {
        @SerialName("items")
        var items: List<VKApiMessage>? = null
    }
}