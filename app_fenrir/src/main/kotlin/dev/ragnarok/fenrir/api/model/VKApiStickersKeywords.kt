package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiStickersKeywords {
    @SerialName("user_stickers")
    var user_stickers: List<VKApiSticker?>? = null

    @SerialName("words")
    var words: List<String?>? = null
}