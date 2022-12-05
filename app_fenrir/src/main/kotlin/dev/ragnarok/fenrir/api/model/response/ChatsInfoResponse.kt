package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.ChatsInfoAdapter
import dev.ragnarok.fenrir.api.model.VKApiChat
import kotlinx.serialization.Serializable

@Serializable(with = ChatsInfoAdapter::class)
class ChatsInfoResponse {
    var chats: List<VKApiChat>? = null
}