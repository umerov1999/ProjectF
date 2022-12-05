package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2Action {
    @SerialName("type")
    var type: String? = null

    @SerialName("target")
    var target: String? = null

    @SerialName("url")
    var url: String? = null
}
