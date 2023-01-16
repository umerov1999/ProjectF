package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2Button {
    @SerialName("action")
    var action: VKApiCatalogV2Action? = null

    @SerialName("name")
    var name: String? = null

    @SerialName("section_id")
    var section_id: String? = null

    @SerialName("title")
    var title: String? = null

    @SerialName("owner_id")
    var owner_id: Long = 0

    @SerialName("target_block_ids")
    var target_block_ids: List<String>? = null

    @SerialName("ref_items_count")
    var ref_items_count = 0

    @SerialName("ref_layout_name")
    var ref_layout_name: String? = null

    @SerialName("ref_data_type")
    var ref_data_type: String? = null
}
