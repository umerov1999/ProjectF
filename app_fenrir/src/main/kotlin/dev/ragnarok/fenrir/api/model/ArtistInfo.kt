package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ArtistInfo {
    @SerialName("domain")
    var domain: String? = null

    @SerialName("id")
    var id: String? = null
}
