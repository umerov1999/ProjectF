package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T> : VkResponse() {
    @SerialName("response")
    var response: T? = null
}