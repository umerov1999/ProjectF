package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FriendsWithCountersResponse {
    @SerialName("friends")
    var friends: Items<VKApiUser>? = null

    @SerialName("counters")
    var counters: VKApiUser.Counters? = null
}