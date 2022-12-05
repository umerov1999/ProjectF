package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiLyrics {
    @SerialName("lyrics_id")
    var lyrics_id = 0

    @SerialName("text")
    var text: String? = null
}