package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Counters<I> {
    @SerialName("cmid")
    var cmid = 0

    @SerialName("counters")
    var counters: ArrayList<I>? = null
}
