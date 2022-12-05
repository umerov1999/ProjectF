package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.CommentsDto
import dev.ragnarok.fenrir.api.model.VKApiTopic
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.JsonObject
import dev.ragnarok.fenrir.util.serializeble.json.intOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive

class TopicDtoAdapter : AbsAdapter<VKApiTopic>("VKApiTopic") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiTopic {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiTopic()
        val root = json.asJsonObject
        dto.id = optInt(root, "id")
        dto.owner_id = optInt(root, "owner_id")
        dto.title = optString(root, "title")
        dto.created = optLong(root, "created")
        dto.created_by = optInt(root, "created_by")
        dto.updated = optInt(root, "updated").toLong()
        dto.updated_by = optInt(root, "updated_by")
        dto.is_closed = optBoolean(root, "is_closed")
        dto.is_fixed = optBoolean(root, "is_fixed")
        if (root.has("comments")) {
            val commentsJson = root["comments"]
            if (commentsJson is JsonObject) {
                dto.comments =
                    kJson.decodeFromJsonElement(
                        CommentsDto.serializer(),
                        commentsJson
                    )
            } else {
                dto.comments = CommentsDto()
                dto.comments?.count = commentsJson?.jsonPrimitive?.intOrNull ?: 0
            }
        }
        dto.first_comment = optString(root, "first_comment")
        dto.last_comment = optString(root, "last_comment")
        return dto
    }

    companion object {
        private val TAG = TopicDtoAdapter::class.java.simpleName
    }
}
