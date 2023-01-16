package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.TopicDtoAdapter
import dev.ragnarok.fenrir.api.model.interfaces.Commentable
import kotlinx.serialization.Serializable

/**
 * An audio object describes an audio file and contains the following fields.
 */
@Serializable(with = TopicDtoAdapter::class)
class VKApiTopic : Commentable {
    var id = 0
    var owner_id = 0L
    var title: String? = null
    var created: Long = 0
    var created_by = 0L
    var updated: Long = 0
    var updated_by = 0L
    var is_closed = false
    var is_fixed = false
    var comments: CommentsDto? = null
    var first_comment: String? = null
    var last_comment: String? = null
}