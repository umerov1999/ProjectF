package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class StoriesResponse {
    @SerialName("items")
    var items: List<StoryBlockResponse>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null
}