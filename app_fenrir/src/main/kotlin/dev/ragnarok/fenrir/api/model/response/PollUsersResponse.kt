package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PollUsersResponse {
    @SerialName("users")
    var users: Items<VKApiUser>? = null
}