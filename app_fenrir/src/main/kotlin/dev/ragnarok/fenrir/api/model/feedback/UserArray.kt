package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.adapters.FeedbackUserArrayDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = FeedbackUserArrayDtoAdapter::class)
class UserArray {
    var count = 0
    var ids: IntArray? = null
}