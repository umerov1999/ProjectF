package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiStickersKeywords {
    @SerialName("words_stickers")
    var words_stickers: List<List<VKApiSticker>?>? = null

    @SerialName("keywords")
    var keywords: List<List<String>?>? = null
}