package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.ChatUserDtoAdapter
import kotlinx.serialization.Serializable

@Serializable(with = ChatUserDtoAdapter::class)
class ChatUserDto {
    var user: VKApiOwner? = null
    var invited_by = 0
    var type: String? = null
}