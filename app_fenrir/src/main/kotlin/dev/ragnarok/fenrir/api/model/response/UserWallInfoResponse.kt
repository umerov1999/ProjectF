package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UserWallInfoResponse {
    @SerialName("user_info")
    var users: List<VKApiUser>? = null

    @SerialName("all_wall_count")
    var allWallCount: Int? = null

    @SerialName("owner_wall_count")
    var ownerWallCount: Int? = null

    @SerialName("postponed_wall_count")
    var postponedWallCount: Int? = null
}