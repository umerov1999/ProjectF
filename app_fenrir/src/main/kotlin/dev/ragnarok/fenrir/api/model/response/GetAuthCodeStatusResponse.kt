package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GetAuthCodeStatusResponse {
    @SerialName("expires_in")
    var expires_in: Long = 0

    @SerialName("access_token")
    var access_token: String? = null

    @SerialName("user_id")
    var user_id: Long = 0

    @SerialName("status")
    var status: Int = 0
}
