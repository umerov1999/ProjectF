package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WallEditResponse {
    @SerialName("post_id")
    var postId: Int? = null
}