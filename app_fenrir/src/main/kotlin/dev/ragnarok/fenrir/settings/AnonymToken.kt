package dev.ragnarok.fenrir.settings

import dev.ragnarok.fenrir.api.model.response.AnonymTokenResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AnonymToken {
    @SerialName("token")
    var token: String? = null
        private set

    @SerialName("expired_at")
    var expired_at: Long = 0
        private set

    fun set(response: AnonymTokenResponse): AnonymToken {
        this.token = response.token
        this.expired_at = response.expired_at
        return this
    }
}