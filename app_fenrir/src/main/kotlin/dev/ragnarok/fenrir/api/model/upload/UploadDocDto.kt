package dev.ragnarok.fenrir.api.model.upload

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UploadDocDto {
    @SerialName("file")
    var file: String? = null
    override fun toString(): String {
        return "UploadDocDto{" +
                "file='" + file + '\'' +
                '}'
    }
}