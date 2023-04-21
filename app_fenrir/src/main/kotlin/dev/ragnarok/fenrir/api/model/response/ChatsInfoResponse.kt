package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.ChatsInfoDtoAdapter
import dev.ragnarok.fenrir.api.model.VKApiChat
import kotlinx.serialization.Serializable

@Serializable(with = ChatsInfoDtoAdapter::class)
class ChatsInfoResponse {
    var chats: List<VKApiChat>? = null
}