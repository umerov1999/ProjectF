package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCheckedLink {
    @SerialName("status")
    var status: String? = null

    @SerialName("link")
    var link: String? = null
}