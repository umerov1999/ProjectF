package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.JsonStringDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = JsonStringDtoAdapter::class)
class VKApiJsonString {
    var json_data: String? = null
}