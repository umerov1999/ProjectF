package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2RecommendedPlaylist : IIdComparable {
    @SerialName("id")
    var id = 0

    @SerialName("owner_id")
    var owner_id = 0L

    @SerialName("percentage")
    var percentage: Double? = null

    @SerialName("percentage_title")
    var percentage_title: String? = null

    @SerialName("color")
    var color: String? = null

    @SerialName("audios")
    var audios: List<String>? = null

    override fun compareFullId(object_s: String): Boolean {
        return (owner_id.toString() + "_" + id) == object_s
    }
}
