package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.response.NewsfeedCommentsResponse.*
import dev.ragnarok.fenrir.kJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

class NewsfeedCommentDtoAdapter : AbsDtoAdapter<Dto>("Dto") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): Dto {
        if (!checkObject(json)) {
            throw UnsupportedOperationException()
        }
        val root = json.jsonObject
        when (optString(root, "type", "post")) {
            "photo" -> {
                return PhotoDto(kJson.decodeFromJsonElement(VKApiPhoto.serializer(), root))
            }

            "post" -> {
                return PostDto(kJson.decodeFromJsonElement(VKApiPost.serializer(), root))
            }

            "video" -> {
                return VideoDto(kJson.decodeFromJsonElement(VKApiVideo.serializer(), root))
            }

            "topic" -> {
                val topic = VKApiTopic()
                topic.id = optInt(root, "post_id")
                if (root.has("to_id")) topic.owner_id = optLong(root, "to_id") else topic.owner_id =
                    optLong(root, "source_id")
                topic.title = optString(root, "text")
                topic.comments =
                    root["comments"]?.let {
                        kJson.decodeFromJsonElement(CommentsDto.serializer(), it)
                    }
                return TopicDto(topic)
            }
        }
        throw UnsupportedOperationException()
    }
}