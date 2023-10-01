package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MessageError {
    @SerialName("code")
    var errorCode = 0

    @SerialName("description")
    var description: String? = null
}
