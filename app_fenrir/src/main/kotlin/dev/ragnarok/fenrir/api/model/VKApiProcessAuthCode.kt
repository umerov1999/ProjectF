package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.ProcessAuthCodeDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = ProcessAuthCodeDtoAdapter::class)
class VKApiProcessAuthCode {
    var status: Int = 0
    var auth_id: String? = null
    var browser_name: String? = null
    var ip: String? = null
    var location: String? = null
}
