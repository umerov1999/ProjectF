package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MessageHistoryResponse {
    @SerialName("items")
    var messages: ArrayList<VKApiMessage>? = null

    @SerialName("count")
    var count = 0

    @SerialName("unread")
    var unread = 0

    @SerialName("conversations")
    var conversations: List<VKApiConversation>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null
}