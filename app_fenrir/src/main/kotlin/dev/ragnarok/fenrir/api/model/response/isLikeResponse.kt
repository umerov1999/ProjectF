package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class IsLikeResponse {
    @SerialName("liked")
    var liked = 0

    @SerialName("copied")
    var copied = 0
}