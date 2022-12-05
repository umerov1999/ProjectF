package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LocalServerSettings {
    @SerialName("url")
    var url: String? = null

    @SerialName("password")
    var password: String? = null

    @SerialName("enabled")
    var enabled = false
}