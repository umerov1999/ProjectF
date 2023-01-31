package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadPhotoToMessageDto {
    @SerialName("server")
    var server = 0L

    @SerialName("photo")
    var photo: String? = null

    @SerialName("hash")
    var hash: String? = null
    override fun toString(): String {
        return "UploadPhotoToMessageDto{" +
                "server=" + server +
                ", photo='" + photo + '\'' +
                ", hash='" + hash + '\'' +
                '}'
    }
}
