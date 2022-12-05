package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RefreshToken {
    @SerialName("token")
    var token: String? = null
}