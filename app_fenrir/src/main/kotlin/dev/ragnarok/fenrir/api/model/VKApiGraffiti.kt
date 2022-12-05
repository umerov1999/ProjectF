package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiGraffiti : VKApiAttachment {
    @SerialName("id")
    var id = 0

    @SerialName("owner_id")
    var owner_id = 0

    @SerialName("url")
    var url: String? = null

    @SerialName("width")
    var width = 0

    @SerialName("height")
    var height = 0

    @SerialName("access_key")
    var access_key: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_GRAFFITI
    }
}