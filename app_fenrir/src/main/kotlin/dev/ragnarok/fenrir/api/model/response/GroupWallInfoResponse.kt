package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class GroupWallInfoResponse {
    @SerialName("group_info")
    var groups: List<VKApiCommunity>? = null

    @SerialName("all_wall_count")
    var allWallCount: Int? = null

    @SerialName("owner_wall_count")
    var ownerWallCount: Int? = null

    @SerialName("suggests_wall_count")
    var suggestsWallCount: Int? = null

    @SerialName("postponed_wall_count")
    var postponedWallCount: Int? = null

    @SerialName("donut_wall_count")
    var donutWallCount: Int? = null
}