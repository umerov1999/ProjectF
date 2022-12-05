package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GroupLongpollServer {
    @SerialName("key")
    var key: String? = null

    @SerialName("server")
    var server: String? = null

    @SerialName("ts")
    var ts: String? = null
}