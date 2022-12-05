package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiArtist {
    @SerialName("domain")
    var domain: String? = null

    @SerialName("id")
    var id: String? = null

    @SerialName("name")
    var name: String? = null
}