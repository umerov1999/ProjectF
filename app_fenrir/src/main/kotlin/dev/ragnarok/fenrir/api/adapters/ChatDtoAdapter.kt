package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.ChatUserDto
import dev.ragnarok.fenrir.api.model.VKApiChat
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.JsonPrimitive
import dev.ragnarok.fenrir.util.serializeble.json.int

class ChatDtoAdapter : AbsAdapter<VKApiChat>("VKApiChat") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiChat {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiChat()
        val root = json.asJsonObject
        dto.id = optInt(root, "id")
        dto.type = optString(root, "type")
        dto.title = optString(root, "title")
        dto.photo_50 = optString(root, "photo_50")
        dto.photo_100 = optString(root, "photo_100")
        dto.photo_200 = optString(root, "photo_200")
        dto.admin_id = optInt(root, "admin_id")
        if (hasArray(root, "users")) {
            val users = root.getAsJsonArray("users")
            dto.users = ArrayList(users?.size.orZero())
            for (i in 0 until users?.size.orZero()) {
                val userElement = users?.get(i)
                if (userElement is JsonPrimitive) {
                    val user = VKApiUser()
                    user.id = userElement.int
                    val chatUserDto = ChatUserDto()
                    chatUserDto.user = user
                    dto.users?.add(chatUserDto)
                } else {
                    if (!checkObject(userElement)) {
                        continue
                    }
                    val jsonObject = userElement.asJsonObject
                    val type = optString(jsonObject, "type")
                    val chatUserDto = ChatUserDto()
                    chatUserDto.type = type
                    chatUserDto.invited_by = optInt(jsonObject, "invited_by", 0)
                    if ("profile" == type) {
                        chatUserDto.user =
                            kJson.decodeFromJsonElement(VKApiUser.serializer(), userElement)
                    } else if ("group" == type) {
                        chatUserDto.user =
                            kJson.decodeFromJsonElement(VKApiCommunity.serializer(), userElement)
                    } else {
                        //not supported
                        continue
                    }
                    dto.users?.add(chatUserDto)
                }
            }
        } else {
            dto.users = ArrayList(0)
        }
        return dto
    }

    companion object {
        private val TAG = ChatDtoAdapter::class.java.simpleName
    }
}