package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ShortVideosResponse {
    @SerialName("items")
    var items: List<VKApiVideo>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null

    @SerialName("next_from")
    var nextFrom: String? = null
}
