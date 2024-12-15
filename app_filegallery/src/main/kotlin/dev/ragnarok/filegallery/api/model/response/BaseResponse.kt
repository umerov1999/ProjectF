package dev.ragnarok.filegallery.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T> : ErrorResponse() {
    @SerialName("response")
    var response: T? = null
}