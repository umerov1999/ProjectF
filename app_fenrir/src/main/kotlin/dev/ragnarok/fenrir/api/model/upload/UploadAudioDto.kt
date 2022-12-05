package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadAudioDto {
    @SerialName("server")
    var server: String? = null

    @SerialName("audio")
    var audio: String? = null

    @SerialName("hash")
    var hash: String? = null
    override fun toString(): String {
        return "UploadAudioDto{" +
                "server='" + server + '\'' +
                ", audio='" + audio + '\'' +
                ", hash='" + hash + '\'' +
                '}'
    }
}