package dev.ragnarok.fenrir.api.model.catalog_v2_audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCatalogV2ArtistItem : IIdComparable {
    @SerialName("name")
    var name: String? = null

    @SerialName("id")
    var id: String? = null

    @SerialName("is_album_cover")
    var is_album_cover: Boolean = false

    @SerialName("photo")
    var photo: List<VKApiCatalogV2Cover>? = null

    @Serializable
    class VKApiCatalogV2Cover {
        @SerialName("width")
        var width: Int = 0

        @SerialName("url")
        var url: String? = null
    }

    override fun compareFullId(object_s: String): Boolean {
        return id == object_s
    }
}
