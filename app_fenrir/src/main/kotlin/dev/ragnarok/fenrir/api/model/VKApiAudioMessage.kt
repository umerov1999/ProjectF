package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiAudioMessage : VKApiAttachment {
    @SerialName("id")
    var id = 0

    @SerialName("owner_id")
    var owner_id = 0

    @SerialName("duration")
    var duration = 0

    @SerialName("waveform")
    var waveform: ByteArray? = null

    @SerialName("link_ogg")
    var linkOgg: String? = null

    @SerialName("link_mp3")
    var linkMp3: String? = null

    @SerialName("access_key")
    var access_key: String? = null

    @SerialName("was_listened")
    var was_listened = false

    @SerialName("transcript")
    var transcript: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_AUDIO_MESSAGE
    }
}