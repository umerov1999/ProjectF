package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Assets<I> {
    @SerialName("version")
    var version = 0

    @SerialName("assets")
    var assets: ArrayList<I>? = null
}
