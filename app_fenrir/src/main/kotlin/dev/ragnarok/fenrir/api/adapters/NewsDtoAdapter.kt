package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject
import dev.ragnarok.fenrir.util.serializeble.json.long

class NewsDtoAdapter : AbsDtoAdapter<VKApiNews>("VKApiNews") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiNews {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiNews()
        val root = json.jsonObject
        dto.type = optString(root, "type")
        dto.source_id = optLong(root, "source_id")
        dto.date = optLong(root, "date")
        dto.post_id = optInt(root, "post_id")
        dto.post_type = optString(root, "post_type")
        dto.final_post = optBoolean(root, "final_post")
        dto.copy_owner_id = optLong(root, "copy_owner_id")
        dto.copy_post_id = optInt(root, "copy_post_id")
        dto.mark_as_ads = optInt(root, "mark_as_ads")
        if (hasArray(root, "copy_history")) {
            dto.copy_history = parseArray(
                root["copy_history"]?.jsonArray,
                emptyList(),
                VKApiPost.serializer()
            )
        } else {
            dto.copy_history = emptyList()
        }
        dto.copy_post_date = optLong(root, "copy_post_date")
        dto.text = optString(root, "text")
        if (hasObject(root, "copyright")) {
            val cop = root["copyright"]?.jsonObject
            val name = optString(cop, "name")
            val link = optString(cop, "link")
            name.nonNullNoEmpty {
                dto.copyright = VKApiNews.Copyright(it, link)
            }
        }
        dto.can_edit = optBoolean(root, "can_edit")
        dto.can_delete = optBoolean(root, "can_delete")
        if (hasObject(root, "comments")) {
            val commentsRoot = root["comments"]?.jsonObject
            dto.comment_count = optInt(commentsRoot, "count")
            dto.comment_can_post = optBoolean(commentsRoot, "can_post")
        }
        if (hasObject(root, "likes")) {
            val likesRoot = root["likes"]?.jsonObject
            dto.like_count = optInt(likesRoot, "count")
            dto.user_like = optBoolean(likesRoot, "user_likes")
            dto.can_like = optBoolean(likesRoot, "can_like")
            dto.can_publish = optBoolean(likesRoot, "can_publish")
        }
        if (hasObject(root, "donut")) {
            val donut = root["donut"]?.jsonObject
            dto.is_donut = optBoolean(donut, "is_donut")
        } else {
            dto.is_donut = false
        }
        if (hasObject(root, "reposts")) {
            val repostsRoot = root["reposts"]?.jsonObject
            dto.reposts_count = optInt(repostsRoot, "count")
            dto.user_reposted = optBoolean(repostsRoot, "user_reposted")
        }
        if (hasObject(root, "views")) {
            val viewRoot = root["views"]?.jsonObject
            dto.views = optInt(viewRoot, "count", 0)
        }
        if (hasArray(root, "attachments")) {
            dto.attachments =
                root["attachments"]?.let {
                    kJson.decodeFromJsonElement(VKApiAttachments.serializer(), it)
                }
        }
        if (hasObject(root, "photos") && hasArray(root["photos"]?.jsonObject, "items")) {
            val photosArray = root["photos"]?.jsonObject?.get("items")?.jsonArray
            if (dto.attachments == null) {
                dto.attachments = VKApiAttachments()
            }
            parseArray(photosArray, null, VKApiPhoto.serializer())?.let {
                dto.attachments?.append(it)
            }
        }
        if (hasObject(root, "photo_tags") && hasArray(root["photo_tags"]?.jsonObject, "items")) {
            val photosTagsArray = root["photo_tags"]?.jsonObject?.get("items")?.jsonArray
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
        if (hasObject(root, "audio") && hasArray(root["audio"]?.jsonObject, "items")) {
            val audiosArray = root["audio"]?.jsonObject?.get("items")?.jsonArray
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
        if (hasObject(root, "video") && hasArray(root["video"]?.jsonObject, "items")) {
            val videoArray = root["video"]?.jsonObject?.get("items")?.jsonArray
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
        if (hasObject(root, "friends") && hasArray(root["friends"]?.jsonObject, "items")) {
            val friendsArray = root["friends"]?.jsonObject?.get("items")?.jsonArray
            dto.friends = ArrayList(friendsArray?.size.orZero())
            for (i in 0 until friendsArray?.size.orZero()) {
                val friendObj = friendsArray?.get(i)?.jsonObject
                friendObj?.get("user_id")?.asPrimitiveSafe?.let { dto.friends?.add(it.long) }
            }
        }
        return dto
    }

    companion object {
        private val TAG = NewsDtoAdapter::class.java.simpleName
    }
}