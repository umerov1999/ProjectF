package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2ListResponse {
    @SerialName("catalog")
    var catalog: CatalogV2Sections? = null

    @Serializable
    class CatalogV2Sections {
        @SerialName("default_section")
        var default_section: String? = null

        @SerialName("sections")
        var sections: List<CatalogV2Section>? = null

        @Serializable
        class CatalogV2Section {
            @SerialName("id")
            var id: String? = null

            @SerialName("title")
            var title: String? = null

            @SerialName("url")
            var url: String? = null
        }
    }
}
