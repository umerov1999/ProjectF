package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.feedback.VKApiBaseFeedback
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NotificationsResponse {
    @SerialName("count")
    var count = 0

    @SerialName("items")
    var notifications: List<VKApiBaseFeedback>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null

    @SerialName("next_from")
    var nextFrom: String? = null

    @SerialName("last_viewed")
    var lastViewed: Long = 0
}