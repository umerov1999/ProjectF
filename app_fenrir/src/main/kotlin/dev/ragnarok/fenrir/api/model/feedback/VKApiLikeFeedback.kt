package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.model.Likeable
import kotlinx.serialization.Serializable

@Serializable
class VKApiLikeFeedback : VKApiBaseFeedback() {
    var users: UserArray? = null
    var liked: Likeable? = null
}