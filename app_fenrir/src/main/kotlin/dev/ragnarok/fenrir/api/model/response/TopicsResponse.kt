package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiTopic
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TopicsResponse {
    @SerialName("count")
    var count = 0

    @SerialName("items")
    var items: List<VKApiTopic>? = null

    @SerialName("default_order")
    var defaultOrder = 0

    @SerialName("can_add_topics")
    var canAddTopics = 0

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null
}