package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AnonymTokenResponse {
    @SerialName("token")
    var token: String? = null

    @SerialName("expired_at")
    var expired_at: Long = 0

    @SerialName("error")
    var error: String? = null

    @SerialName("error_description")
    var errorDescription: String? = null
}