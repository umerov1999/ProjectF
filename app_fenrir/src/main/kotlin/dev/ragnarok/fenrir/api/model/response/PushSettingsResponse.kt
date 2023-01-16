package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.response.PushSettingsResponse.ConversationsPush.ConversationPushItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PushSettingsResponse {
    @SerialName("conversations")
    var conversations: ConversationsPush? = null
    val pushSettings: List<ConversationPushItem>
        get() = conversations?.items ?: emptyList()

    @Serializable
    class ConversationsPush {
        @SerialName("items")
        var items: List<ConversationPushItem>? = null

        @Serializable
        class ConversationPushItem {
            @SerialName("disabled_until")
            var disabled_until = 0L

            @SerialName("peer_id")
            var peer_id = 0L
        }
    }
}