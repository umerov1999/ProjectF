package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadPhotoToAlbumDto {
    @SerialName("server")
    var server = 0

    @SerialName("photos_list")
    var photos_list: String? = null

    @SerialName("aid")
    var aid = 0L

    @SerialName("hash")
    var hash: String? = null
    override fun toString(): String {
        return "UploadPhotoToAlbumDto{" +
                "server=" + server +
                ", photos_list='" + photos_list + '\'' +
                ", aid=" + aid +
                ", hash='" + hash + '\'' +
                '}'
    }
}