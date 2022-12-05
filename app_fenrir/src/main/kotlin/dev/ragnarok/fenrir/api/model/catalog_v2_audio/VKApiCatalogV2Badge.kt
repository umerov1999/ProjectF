package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2Badge {
    @SerialName("text")
    var text: String? = null

    @SerialName("type")
    var type: String? = null
}
