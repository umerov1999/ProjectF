package dev.ragnarok.filegallery.api.model.response

import dev.ragnarok.filegallery.api.model.Error
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class ErrorResponse {
    @SerialName("error")
    var error: Error? = null
}