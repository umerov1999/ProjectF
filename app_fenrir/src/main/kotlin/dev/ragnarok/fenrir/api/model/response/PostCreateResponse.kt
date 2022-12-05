package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PostCreateResponse {
    @SerialName("post_id")
    var postId = 0
}