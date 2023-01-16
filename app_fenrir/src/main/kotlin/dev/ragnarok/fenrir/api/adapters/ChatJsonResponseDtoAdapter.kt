package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiMessage
import dev.ragnarok.fenrir.api.model.local_json.ChatJsonResponse
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class ChatJsonResponseDtoAdapter : AbsAdapter<ChatJsonResponse>("ChatJsonResponse") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): ChatJsonResponse {
        val story = ChatJsonResponse()
        if (!checkObject(json)) {
            return story
        }
        val root = json.asJsonObject
        story.type = optString(root, "type")
        story.page_avatar = optString(root, "page_avatar")
        story.page_id = optLong(root, "page_id")
        story.page_instagram = optString(root, "page_instagram")
        story.page_phone_number = optString(root, "page_phone_number")
        story.page_site = optString(root, "page_site")
        story.page_title = optString(root, "page_title")
        story.version = root["version"]?.let {
            kJson.decodeFromJsonElement(ChatJsonResponse.Version.serializer(), it)
        }
        story.messages = parseArray(
            story.type?.let { root.getAsJsonArray(it) },
            emptyList(), VKApiMessage.serializer()
        )
        return story
    }
}