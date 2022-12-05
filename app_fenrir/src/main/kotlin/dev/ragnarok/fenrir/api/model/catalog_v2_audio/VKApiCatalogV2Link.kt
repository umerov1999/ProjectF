package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import dev.ragnarok.fenrir.api.adapters.catalog_v2_audio.VKApiCatalogV2LinkDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = VKApiCatalogV2LinkDtoAdapter::class)
class VKApiCatalogV2Link : IIdComparable {
    var id: String? = null
    var url: String? = null
    var title: String? = null
    var subtitle: String? = null
    var preview_photo: String? = null

    override fun compareFullId(object_s: String): Boolean {
        return id == object_s
    }
}