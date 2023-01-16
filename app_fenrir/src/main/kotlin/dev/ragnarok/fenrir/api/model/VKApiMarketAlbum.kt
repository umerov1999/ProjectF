package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiMarketAlbum : VKApiAttachment {
    @SerialName("id")
    var id = 0

    @SerialName("owner_id")
    var owner_id = 0L

    @SerialName("access_key")
    var access_key: String? = null

    @SerialName("title")
    var title: String? = null

    @SerialName("photo")
    var photo: VKApiPhoto? = null

    @SerialName("count")
    var count = 0

    @SerialName("updated_time")
    var updated_time: Long = 0
    override fun getType(): String {
        return VKApiAttachment.TYPE_MARKET_ALBUM
    }
}