package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.Error
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class VkResponse {
    @SerialName("error")
    var error: Error? = null

    /*
    @SerialName("execute_errors")
    var executeErrors: List<Error>? = null
     */
}