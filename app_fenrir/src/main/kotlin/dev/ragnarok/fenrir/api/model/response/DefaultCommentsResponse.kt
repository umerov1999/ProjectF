package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DefaultCommentsResponse {
    @SerialName("count")
    var count = 0

    @SerialName("items")
    var items: List<VKApiComment>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null
}