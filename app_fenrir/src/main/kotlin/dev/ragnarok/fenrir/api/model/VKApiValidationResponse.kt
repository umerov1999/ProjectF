package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiValidationResponse {
    @SerialName("sid")
    var sid: String? = null

    @SerialName("delay")
    var delay = 0

    @SerialName("validation_type")
    var validation_type: String? = null

    @SerialName("validation_resend")
    var validation_resend: String? = null
}