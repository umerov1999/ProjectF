package dev.ragnarok.fenrir.api.model.local_json

import dev.ragnarok.fenrir.api.adapters.ChatJsonResponseDtoAdapter
import dev.ragnarok.fenrir.api.model.VKApiMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = ChatJsonResponseDtoAdapter::class)
class ChatJsonResponse {
    var type: String? = null
    var messages: List<VKApiMessage>? = null
    var version: Version? = null
    var page_id = 0L
    var page_title: String? = null
    var page_avatar: String? = null
    var page_phone_number: String? = null
    var page_instagram: String? = null
    var page_site: String? = null

    @Suppress("unused")
    @Serializable
    class Version {
        @SerialName("float")
        var floatValue = 0f

        @SerialName("string")
        var stringValue = 0f
    }
}