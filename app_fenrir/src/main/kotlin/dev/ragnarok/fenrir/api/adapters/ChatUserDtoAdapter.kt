package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.ChatUserDto
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.kJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

class ChatUserDtoAdapter : AbsDtoAdapter<ChatUserDto>("ChatUserDto") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): ChatUserDto {
        val user: VKApiUser =
            kJson.decodeFromJsonElement(VKApiUser.serializer(), json)
        val dto = ChatUserDto()
        if (checkObject(json)) {
            val root = json.jsonObject
            dto.invited_by = optLong(root, "invited_by")
            dto.type = optString(root, "type")
        }
        dto.user = user
        return dto
    }
}