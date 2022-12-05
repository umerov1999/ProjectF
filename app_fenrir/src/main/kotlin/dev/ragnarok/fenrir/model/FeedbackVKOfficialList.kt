package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.api.adapters.FeedbackVKOfficialDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = FeedbackVKOfficialDtoAdapter::class)
class FeedbackVKOfficialList {
    var items: ArrayList<FeedbackVKOfficial>? = null
}