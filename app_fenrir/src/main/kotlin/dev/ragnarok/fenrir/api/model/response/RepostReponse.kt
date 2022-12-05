package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RepostReponse {
    @SerialName("post_id")
    var postId: Int? = null

    @SerialName("reposts_count")
    var repostsCount: Int? = null

    @SerialName("likes_count")
    var likesCount: Int? = null
}