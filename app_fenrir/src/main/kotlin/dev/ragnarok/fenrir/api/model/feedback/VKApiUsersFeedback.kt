package dev.ragnarok.fenrir.api.model.feedback

import kotlinx.serialization.Serializable

@Serializable
class VKApiUsersFeedback : VKApiBaseFeedback() {
    var users: UserArray? = null
}