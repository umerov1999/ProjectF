package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.LyricsAdapter
import kotlinx.serialization.Serializable

@Serializable(with = LyricsAdapter::class)
class VKApiLyrics {
    var text: String? = null
}