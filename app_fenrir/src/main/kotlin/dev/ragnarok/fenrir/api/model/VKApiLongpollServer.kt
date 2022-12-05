package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiLongpollServer {
    @SerialName("key")
    var key: String? = null

    @SerialName("server")
    var server: String? = null

    @SerialName("ts")
    var ts: Long = 0

    @SerialName("pts")
    var pts: Long = 0
}