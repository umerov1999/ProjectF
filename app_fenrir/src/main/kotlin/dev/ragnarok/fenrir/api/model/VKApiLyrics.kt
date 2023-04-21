package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.LyricsDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = LyricsDtoAdapter::class)
class VKApiLyrics {
    var text: String? = null
}