package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadStoryDto {
    @SerialName("upload_result")
    var upload_result: String? = null
    override fun toString(): String {
        return "UploadStoryDto{" +
                "upload_result='" + upload_result + '\'' +
                '}'
    }
}