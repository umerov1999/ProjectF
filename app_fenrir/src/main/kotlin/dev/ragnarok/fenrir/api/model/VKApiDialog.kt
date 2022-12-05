package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiDialog {
    @SerialName("conversation")
    var conversation: VKApiConversation? = null

    @SerialName("last_message")
    var lastMessage: VKApiMessage? = null
}
