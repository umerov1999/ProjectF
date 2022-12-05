package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.int
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive

class NewsAdapter : AbsAdapter<VKApiNews>("VKApiNews") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiNews {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiNews()
        val root = json.asJsonObject
        dto.type = optString(root, "type")
        dto.source_id = optInt(root, "source_id")
        dto.date = optLong(root, "date")
        dto.post_id = optInt(root, "post_id")
        dto.post_type = optString(root, "post_type")
        dto.final_post = optBoolean(root, "final_post")
        dto.copy_owner_id = optInt(root, "copy_owner_id")
        dto.copy_post_id = optInt(root, "copy_post_id")
        dto.mark_as_ads = optInt(root, "mark_as_ads")
        if (hasArray(root, "copy_history")) {
            dto.copy_history = parseArray(
                root.getAsJsonArray("copy_history"),
                emptyList(),
                VKApiPost.serializer()
            )
        } else {
            dto.copy_history = emptyList()
        }
        dto.copy_post_date = optLong(root, "copy_post_date")
        dto.text = optString(root, "text")
        if (hasObject(root, "copyright")) {
            val cop = root.getAsJsonObject("copyright")
            val name = optString(cop, "name")
            val link = optString(cop, "link")
            name.nonNullNoEmpty {
                dto.copyright = VKApiNews.Copyright(it, link)
            }
        }
        dto.can_edit = optBoolean(root, "can_edit")
        dto.can_delete = optBoolean(root, "can_delete")
        if (hasObject(root, "comments")) {
            val commentsRoot = root.getAsJsonObject("comments")
            dto.comment_count = optInt(commentsRoot, "count")
            dto.comment_can_post = optBoolean(commentsRoot, "can_post")
        }
        if (hasObject(root, "likes")) {
            val likesRoot = root.getAsJsonObject("likes")
            dto.like_count = optInt(likesRoot, "count")
            dto.user_like = optBoolean(likesRoot, "user_likes")
            dto.can_like = optBoolean(likesRoot, "can_like")
            dto.can_publish = optBoolean(likesRoot, "can_publish")
        }
        if (hasObject(root, "reposts")) {
            val repostsRoot = root.getAsJsonObject("reposts")
            dto.reposts_count = optInt(repostsRoot, "count")
            dto.user_reposted = optBoolean(repostsRoot, "user_reposted")
        }
        if (hasObject(root, "views")) {
            val viewRoot = root.getAsJsonObject("views")
            dto.views = optInt(viewRoot, "count", 0)
        }
        if (hasArray(root, "attachments")) {
            dto.attachments =
                root["attachments"]?.let {
                    kJson.decodeFromJsonElement(VKApiAttachments.serializer(), it)
                }
        }
        if (root.has("photos")) {
            val photosArray = root.getAsJsonObject("photos").getAsJsonArray("items")
            if (dto.attachments == null) {
                dto.attachments = VKApiAttachments()
            }
            parseArray(photosArray, null, VKApiPhoto.serializer())?.let {
                dto.attachments?.append(it)
            }
        }
        if (root.has("photo_tags")) {
            val photosTagsArray = root.getAsJsonObject("photo_tags").getAsJsonArray("items")
            if (dto.attachments == null) {
                dto.attachments = VKApiAttachments()
            }
            parseArray(
                photosTagsArray,
                null, VKApiPhoto.serializer()
            )?.let {
                dto.attachments?.append(it)
            }
        }
        if (root.has("audio")) {
            val audiosArray = root.getAsJsonObject("audio").getAsJsonArray("items")
            if (dto.attachments == null) {
                dto.attachments = VKApiAttachments()
            }
            parseArray(
                audiosArray,
                null,
                VKApiAudio.serializer()
            )?.let {
                dto.attachments?.append(it)
            }
        }
        if (root.has("video")) {
            val videoArray = root.getAsJsonObject("video").getAsJsonArray("items")
            if (dto.attachments == null) {
                dto.attachments = VKApiAttachments()
            }
            parseArray(
                videoArray,
                null, VKApiVideo.serializer()
            )?.let {
                dto.attachments?.append(it)
            }
        }
        if (root.has("friends")) {
            val friendsArray = root.getAsJsonObject("friends").getAsJsonArray("items")
            dto.friends = ArrayList(friendsArray?.size.orZero())
            for (i in 0 until friendsArray?.size.orZero()) {
                val friendObj = friendsArray?.get(i)?.asJsonObject
                friendObj?.get("user_id")?.jsonPrimitive?.let { dto.friends?.add(it.int) }
            }
        }
        return dto
    }

    companion object {
        private val TAG = NewsAdapter::class.java.simpleName
    }
}