package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiEvent : VKApiAttachment {
    @SerialName("id")
    var id = 0

    @SerialName("button_text")
    var button_text: String? = null

    @SerialName("text")
    var text: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_EVENT
    }
}