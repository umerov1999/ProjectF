package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadChatPhotoDto {
    @SerialName("response")
    var response: String? = null
    override fun toString(): String {
        return "UploadChatPhotoDto{" +
                "response='" + response + '\'' +
                '}'
    }
}