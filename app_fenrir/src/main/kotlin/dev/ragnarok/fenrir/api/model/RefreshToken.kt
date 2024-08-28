package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RefreshToken {
    @SerialName("token")
    var token: String? = null

    @SerialName("expired_at")
    var expired_at: Long = 0
}