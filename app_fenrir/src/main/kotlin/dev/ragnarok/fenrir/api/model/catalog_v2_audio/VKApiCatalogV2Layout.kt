package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2Layout {
    @SerialName("name")
    var name: String? = null

    @SerialName("title")
    var title: String? = null

    @SerialName("top_title")
    var top_title: VKApiCatalogV2TopTitle? = null
}