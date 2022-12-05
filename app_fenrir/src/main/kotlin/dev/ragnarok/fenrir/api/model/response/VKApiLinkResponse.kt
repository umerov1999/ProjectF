package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiLinkResponse {
    @SerialName("link")
    var link: String? = null
}