package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiFeedList {
    @SerialName("id")
    var id = 0

    @SerialName("title")
    var title: String? = null

    @SerialName("no_reposts")
    var no_reposts = false

    @SerialName("source_ids")
    var source_ids: IntArray? = null
}