package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiShortLink {
    @SerialName("short_url")
    var short_url: String? = null

    @SerialName("url")
    var url: String? = null

    @SerialName("timestamp")
    var timestamp: Long = 0

    @SerialName("access_key")
    var access_key: String? = null

    @SerialName("key")
    var key: String? = null

    @SerialName("views")
    var views = 0
}