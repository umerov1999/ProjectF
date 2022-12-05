package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadPhotoToWallDto {
    @SerialName("server")
    var server = 0

    @SerialName("photo")
    var photo: String? = null

    @SerialName("hash")
    var hash: String? = null
    override fun toString(): String {
        return "UploadPhotoToWallDto{" +
                "server=" + server +
                ", photo='" + photo + '\'' +
                ", hash='" + hash + '\'' +
                '}'
    }
}