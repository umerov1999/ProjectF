package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LikeResponse {
    @SerialName("likes")
    var likes = 0
}