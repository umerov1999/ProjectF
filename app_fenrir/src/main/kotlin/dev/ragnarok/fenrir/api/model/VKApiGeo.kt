package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.GeoDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = GeoDtoAdapter::class)
class VKApiGeo : VKApiAttachment {
    var latitude: String? = null
    var longitude: String? = null
    var title: String? = null
    var address: String? = null
    var country: Int = 0
    var id: Int = 0

    override fun getType(): String {
        return VKApiAttachment.TYPE_GEO
    }
}
