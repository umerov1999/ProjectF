package dev.ragnarok.fenrir.api.model.feedback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Copies {
    @SerialName("count")
    var count = 0

    @SerialName("items")
    var pairs: List<IdPair>? = null

    @Serializable
    class IdPair {
        @SerialName("id")
        var id = 0

        @SerialName("from_id")
        var owner_id = 0
    }
}