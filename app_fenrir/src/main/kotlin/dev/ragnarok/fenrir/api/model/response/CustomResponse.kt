package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CustomResponse<T> {
    @SerialName("response")
    var response: T? = null
    var error: String? = null
    var error_code: Int? = null
}