package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2TopTitle {
    @SerialName("icon")
    var icon: String? = null

    @SerialName("text")
    var text: String? = null
}
