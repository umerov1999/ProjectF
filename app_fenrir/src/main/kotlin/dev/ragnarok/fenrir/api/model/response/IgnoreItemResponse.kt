package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class IgnoreItemResponse {
    @SerialName("status")
    var status = false

    @SerialName("message")
    var message: String? = null
}
