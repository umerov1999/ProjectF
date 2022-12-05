package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AddToPlaylistResponse {
    @SerialName("audio_id")
    var audio_id = 0
}