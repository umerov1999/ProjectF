package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.ProfileInfoResponseDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = ProfileInfoResponseDtoAdapter::class)
class VKApiProfileInfoResponse {
    var status = 0
}