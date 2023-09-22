package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.NarrativesDtoAdapter
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

@Serializable(with = NarrativesDtoAdapter::class)
class VKApiNarratives : VKApiAttachment {
    var id = 0
    var owner_id = 0L
    var title: String? = null
    var access_key: String? = null
    var story_ids: IntArray? = null
    var cover: String? = null

    override fun getType(): String {
        return VKApiAttachment.TYPE_NARRATIVE
    }
}