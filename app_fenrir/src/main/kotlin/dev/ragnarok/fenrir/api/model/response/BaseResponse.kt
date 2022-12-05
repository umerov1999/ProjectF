package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.Error
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BaseResponse<T> : VkResponse() {
    @SerialName("response")
    var response: T? = null

    @SerialName("execute_errors")
    var executeErrors: List<Error>? = null
}