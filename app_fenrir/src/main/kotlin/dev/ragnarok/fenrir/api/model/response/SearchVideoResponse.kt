package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.VKApiVideo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SearchVideoResponse {
    @SerialName("count")
    var count = 0

    @SerialName("items")
    var items: List<VKApiVideo>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null
}