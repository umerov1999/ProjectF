package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiStickerSet : Identificable {
    @SerialName("background")
    var background: String? = null

    @SerialName("description")
    var description: String? = null

    @SerialName("author")
    var author: String? = null

    @SerialName("free")
    var free = false

    @SerialName("can_purchase")
    var can_purchase = false

    @SerialName("payment_type")
    var payment_type: String? = null

    @SerialName("product")
    var product: Product? = null
    override fun getObjectId(): Int {
        return product?.id.orZero()
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
    class Product {
        @SerialName("id")
        var id = 0

        @SerialName("purchased")
        var purchased = false

        @SerialName("title")
        var title: String? = null

        @SerialName("promoted")
        var promoted = false

        @SerialName("active")
        var active = false

        @SerialName("type")
        var type: String? = null

        @SerialName("icon")
        var icon: List<Image>? = null

        @SerialName("stickers")
        var stickers: List<VKApiSticker>? = null
    }
}