package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.ChatUserDto
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class ChatUserDtoAdapter : AbsAdapter<ChatUserDto>("ChatUserDto") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): ChatUserDto {
        val user: VKApiUser =
            kJson.decodeFromJsonElement(VKApiUser.serializer(), json)
        val dto = ChatUserDto()
        if (checkObject(json)) {
            val root = json.asJsonObject
            dto.invited_by = optInt(root, "invited_by")
            dto.type = optString(root, "type")
        }
        dto.user = user
        return dto
    }
}