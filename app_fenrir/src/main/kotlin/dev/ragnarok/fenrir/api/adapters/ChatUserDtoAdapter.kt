package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.ChatUserDto
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.kJson
import kotlinx.serialization.json.JsonElement

class ChatUserDtoAdapter : AbsDtoAdapter<ChatUserDto>("ChatUserDto") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): ChatUserDto {
        val dto = ChatUserDto()
        if (checkObject(json)) {
            val user: VKApiUser =
                kJson.decodeFromJsonElement(VKApiUser.serializer(), json)
            dto.user = user
            dto.invited_by = optLong(json, "invited_by")
            dto.type = optString(json, "type")
        }
        return dto
    }
}