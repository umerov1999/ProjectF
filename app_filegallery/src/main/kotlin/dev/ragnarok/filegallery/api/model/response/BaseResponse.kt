package dev.ragnarok.filegallery.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T> : ErrorReponse() {
    @SerialName("response")
    var response: T? = null
}