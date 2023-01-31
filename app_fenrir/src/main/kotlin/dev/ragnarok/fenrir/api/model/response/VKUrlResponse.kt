package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
open class VKUrlResponse {
    @SerialName("error")
    var error: String? = null

    @SerialName("error_description")
    var errorDescription: String? = null

    @Transient
    var resultUrl: String? = null
}
