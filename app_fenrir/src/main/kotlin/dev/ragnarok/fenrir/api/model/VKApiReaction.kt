package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class VKApiReaction {
    var count: Int = 0
    var reaction_id = 0
}