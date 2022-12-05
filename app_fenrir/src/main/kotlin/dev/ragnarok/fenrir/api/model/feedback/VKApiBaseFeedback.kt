package dev.ragnarok.fenrir.api.model.feedback

import dev.ragnarok.fenrir.api.adapters.FeedbackDtoAdapter
import dev.ragnarok.fenrir.api.model.VKApiComment
import kotlinx.serialization.Serializable

@Serializable(with = FeedbackDtoAdapter::class)
abstract class VKApiBaseFeedback {
    var type: String? = null
    var date: Long = 0
    var reply: VKApiComment? = null
}