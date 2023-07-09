package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2Section {
    @SerialName("blocks")
    var blocks: List<VKApiCatalogV2Block>? = null

    //@SerialName("buttons")
    //var buttons: List<VKApiCatalogV2Button>? = null

    @SerialName("id")
    var id: String? = null

    @SerialName("title")
    var title: String? = null

    @SerialName("url")
    var url: String? = null

    @SerialName("next_from")
    var next_from: String? = null
}
