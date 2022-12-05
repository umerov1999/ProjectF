package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.model.VKApiPost
import kotlinx.serialization.Serializable

/**
 * Base class for types [mention_comments, mention, mention_comment_photo, mention_comment_video]
 */
@Serializable
class VKApiMentionWallFeedback : VKApiBaseFeedback() {
    var post: VKApiPost? = null
}