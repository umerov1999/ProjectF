package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiAttachments
import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject
import kotlinx.serialization.builtins.ListSerializer

class CommentDtoAdapter : AbsDtoAdapter<VKApiComment>("VKApiComment") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiComment {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiComment()
        val root = json.jsonObject
        dto.id = optInt(root, "id")
        dto.from_id = optLong(root, "from_id")
        if (dto.from_id == 0L) {
            dto.from_id = optLong(root, "owner_id")
        }
        dto.date = optLong(root, "date")
        dto.text = optString(root, "text")
        dto.reply_to_user = optLong(root, "reply_to_user")
        dto.reply_to_comment = optInt(root, "reply_to_comment")
        if (hasArray(root, "attachments")) {
            dto.attachments = root["attachments"]?.let {
                kJson.decodeFromJsonElement(VKApiAttachments.serializer(), it)
            }
        }
        if (hasObject(root, "thread")) {
            val threadRoot = root["thread"]?.jsonObject
            dto.threads_count = optInt(threadRoot, "count")
            if (hasArray(threadRoot, "items")) {
                dto.threads = threadRoot["items"]?.let {
                    kJson.decodeFromJsonElement(ListSerializer(VKApiComment.serializer()), it)
                }
            }
        }
        dto.pid = optInt(root, "pid")
        if (hasObject(root, "likes")) {
            val likesRoot = root["likes"]?.jsonObject
            dto.likes = optInt(likesRoot, "count")
            dto.user_likes = optBoolean(likesRoot, "user_likes")
            dto.can_like = optBoolean(likesRoot, "can_like")
        }
        dto.can_edit = optBoolean(root, "can_edit")
        return dto
    }

    companion object {
        private val TAG = CommentDtoAdapter::class.java.simpleName
    }
}