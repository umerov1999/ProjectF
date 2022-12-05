package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiAttachments
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AttachmentsHistoryResponse {
    @SerialName("items")
    var items: List<One>? = null

    @SerialName("next_from")
    var next_from: String? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null

    @Serializable
    class One {
        @SerialName("message_id")
        var messageId = 0

        @SerialName("attachment")
        var entry: VKApiAttachments.Entry? = null
    }
}