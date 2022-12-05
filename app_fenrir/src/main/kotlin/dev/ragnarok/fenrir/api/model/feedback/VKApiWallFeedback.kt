package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.model.VKApiPost
import kotlinx.serialization.Serializable

@Serializable
class VKApiWallFeedback : VKApiBaseFeedback() {
    var post: VKApiPost? = null
}