package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CommentsDto {
    @SerialName("count")
    var count = 0

    @SerialName("can_post")
    var canPost = false

    @SerialName("list")
    var list: List<VKApiComment>? = null
}