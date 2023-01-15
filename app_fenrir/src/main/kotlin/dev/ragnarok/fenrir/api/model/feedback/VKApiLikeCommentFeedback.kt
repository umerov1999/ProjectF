package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.api.model.interfaces.Commentable
import kotlinx.serialization.Serializable

@Serializable
class VKApiLikeCommentFeedback : VKApiBaseFeedback() {
    var users: UserArray? = null
    var comment: VKApiComment? = null
    var commented: Commentable? = null
}