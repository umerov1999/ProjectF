package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T> : VKResponse() {
    @SerialName("response")
    var response: T? = null
}