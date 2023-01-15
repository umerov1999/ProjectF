package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes a photo object from VK.
 */
@Serializable
class VKApiSticker : VKApiAttachment {
    /**
     * Sticker ID, positive number
     */
    var sticker_id = 0

    @SerialName("images")
    var images: List<Image>? = null

    @SerialName("images_with_background")
    var images_with_background: List<Image>? = null

    @SerialName("animation_url")
    var animation_url: String? = null

    @SerialName("animations")
    var animations: List<VKApiAnimation>? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_STICKER
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

    @Serializable
    class VKApiAnimation {
        @SerialName("type")
        var type: String? = null

        @SerialName("url")
        var url: String? = null
    }
}