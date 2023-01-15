package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.api.model.interfaces.Commentable
import kotlinx.serialization.Serializable

@Serializable
class VKApiReplyCommentFeedback : VKApiBaseFeedback() {
    var comments_of: Commentable? = null
    var own_comment: VKApiComment? = null
    var feedback_comment: VKApiComment? = null
}