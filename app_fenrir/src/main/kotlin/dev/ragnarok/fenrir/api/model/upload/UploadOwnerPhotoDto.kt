package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadOwnerPhotoDto {
    @SerialName("server")
    var server: String? = null

    @SerialName("photo")
    var photo: String? = null

    @SerialName("hash")
    var hash: String? = null
    override fun toString(): String {
        return "UploadOwnerPhotoDto{" +
                "server=" + server +
                ", photo='" + photo + '\'' +
                ", hash='" + hash + '\'' +
                '}'
    }
}