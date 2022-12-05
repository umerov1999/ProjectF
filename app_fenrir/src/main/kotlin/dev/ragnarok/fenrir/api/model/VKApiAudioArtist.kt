package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiAudioArtist : VKApiAttachment {
    @SerialName("id")
    var id: String? = null

    @SerialName("name")
    var name: String? = null

    @SerialName("photo")
    var photo: List<Image>? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_ARTIST
    }

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