package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BlockResponse<T> {
    @SerialName("block")
    var block: T? = null
}