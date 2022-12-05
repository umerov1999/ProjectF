package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadVideoDto {
    @SerialName("owner_id")
    var owner_id = 0

    @SerialName("video_id")
    var video_id = 0

    @SerialName("video_hash")
    var video_hash: String? = null
    override fun toString(): String {
        return "UploadVideoDto{" +
                "owner_id=" + owner_id +
                ", video_id='" + video_id + '\'' +
                ", video_hash='" + video_hash + '\'' +
                '}'
    }
}