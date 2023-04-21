package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.CommentsDto
import dev.ragnarok.fenrir.api.model.VKApiTopic
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.intOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class TopicDtoAdapter : AbsDtoAdapter<VKApiTopic>("VKApiTopic") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiTopic {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiTopic()
        val root = json.jsonObject
        dto.id = optInt(root, "id")
        dto.owner_id = optLong(root, "owner_id")
        dto.title = optString(root, "title")
        dto.created = optLong(root, "created")
        dto.created_by = optLong(root, "created_by")
        dto.updated = optLong(root, "updated")
        dto.updated_by = optLong(root, "updated_by")
        dto.is_closed = optBoolean(root, "is_closed")
        dto.is_fixed = optBoolean(root, "is_fixed")
        if (root.has("comments")) {
            val commentsJson = root["comments"]
            if (checkObject(commentsJson)) {
                dto.comments =
                    kJson.decodeFromJsonElement(
                        CommentsDto.serializer(),
                        commentsJson
                    )
            } else {
                dto.comments = CommentsDto()
                dto.comments?.count = commentsJson?.asPrimitiveSafe?.intOrNull ?: 0
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
