package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.FaveLinkDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = FaveLinkDtoAdapter::class)
class FaveLinkDto {
    var id: String? = null
    var url: String? = null
    var title: String? = null
    var description: String? = null
    var photo: VKApiPhoto? = null
}