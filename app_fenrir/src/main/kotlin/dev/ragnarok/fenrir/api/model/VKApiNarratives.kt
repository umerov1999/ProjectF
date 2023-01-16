package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.NarrativesDtoAdapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = NarrativesDtoAdapter::class)
class VKApiNarratives {
    @SerialName("id")
    var id = 0

    @SerialName("owner_id")
    var owner_id = 0L

    @SerialName("title")
    var title: String? = null

    @SerialName("story_ids")
    var story_ids: IntArray? = null

    @SerialName("cover")
    var cover: String? = null
}