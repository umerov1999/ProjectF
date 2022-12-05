package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FavePageResponse {
    @SerialName("description")
    var description: String? = null

    @SerialName("type")
    var type: String? = null

    @SerialName("updated_date")
    var updated_date: Long = 0

    @SerialName("user")
    var user: VKApiUser? = null

    @SerialName("group")
    var group: VKApiCommunity? = null
}