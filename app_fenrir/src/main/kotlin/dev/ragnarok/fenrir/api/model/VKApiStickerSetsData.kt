package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiStickerSetsData {
    @SerialName("recent")
    var recent: Items<VKApiSticker>? = null

    @SerialName("sticker_pack")
    var sticker_pack: Items<VKApiStickerSet.Product>? = null
}