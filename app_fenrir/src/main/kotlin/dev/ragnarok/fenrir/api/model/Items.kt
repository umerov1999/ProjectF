package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Items<I> {
    @SerialName("count")
    var count = 0

    @SerialName("items")
    var items: ArrayList<I>? = null
}