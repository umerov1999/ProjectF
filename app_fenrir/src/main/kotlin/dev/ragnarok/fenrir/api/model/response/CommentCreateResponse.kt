package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CommentCreateResponse {
    @SerialName("comment_id")
    var commentId = 0
}