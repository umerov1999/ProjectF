package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class OnlineFriendsResponse {
    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null
}