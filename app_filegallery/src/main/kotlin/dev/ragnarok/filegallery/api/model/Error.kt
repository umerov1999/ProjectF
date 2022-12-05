package dev.ragnarok.filegallery.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Error {
    @SerialName("error_msg")
    var errorMsg: String? = null

    @SerialName("method")
    var method: String? = null
}