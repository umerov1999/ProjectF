package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCover {
    @SerialName("enabled")
    var enabled = false

    @SerialName("images")
    var images: List<Image>? = null

    @Serializable
    class Image {
        @SerialName("url")
        var url: String? = null

        @SerialName("width")
        var width = 0

        @SerialName("height")
        var height = 0
    }
}