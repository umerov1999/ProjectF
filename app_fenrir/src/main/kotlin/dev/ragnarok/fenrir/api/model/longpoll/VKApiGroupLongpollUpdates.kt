package dev.ragnarok.fenrir.api.model.longpoll

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiGroupLongpollUpdates {
    @SerialName("failed")
    var failed = 0

    @SerialName("ts")
    var ts: String? = null
    val count: Int
        get() = 0
}