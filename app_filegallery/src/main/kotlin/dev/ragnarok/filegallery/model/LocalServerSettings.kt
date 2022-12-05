package dev.ragnarok.filegallery.model

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

    @SerialName("enabled_audio_local_sync")
    var enabled_audio_local_sync = false
}