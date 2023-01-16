package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class VKApiGift {
    var id = 0
    var from_id = 0L
    var message: String? = null
    var date: Long = 0
    var gift: VKApiGiftItem? = null
    var privacy = 0
}