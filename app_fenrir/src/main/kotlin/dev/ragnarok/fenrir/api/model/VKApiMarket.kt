package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.MarketDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = MarketDtoAdapter::class)
class VKApiMarket : VKApiAttachment {
    var id = 0
    var owner_id = 0
    var access_key: String? = null
    var weight = 0
    var availability = 0
    var date: Long = 0
    var is_favorite = false
    var title: String? = null
    var description: String? = null
    var price: String? = null
    var dimensions: String? = null
    var thumb_photo: String? = null
    var sku: String? = null
    var photos: ArrayList<VKApiPhoto>? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_MARKET
    }
}