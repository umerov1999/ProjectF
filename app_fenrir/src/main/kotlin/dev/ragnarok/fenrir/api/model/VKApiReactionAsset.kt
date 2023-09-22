package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.ReactionAssetDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = ReactionAssetDtoAdapter::class)
class VKApiReactionAsset {
    var big_animation: String? = null
    var small_animation: String? = null
    var static: String? = null
    var reaction_id = 0
}
