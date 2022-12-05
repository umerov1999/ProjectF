package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.model.Commentable
import dev.ragnarok.fenrir.api.model.VKApiComment
import kotlinx.serialization.Serializable

@Serializable
class VKApiMentionCommentFeedback : VKApiBaseFeedback() {
    var where: VKApiComment? = null
    var comment_of: Commentable? = null
}