package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.VKApiPost.Type.DONUT
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class PostDtoAdapter : AbsDtoAdapter<VKApiPost>("VKApiPost") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiPost {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiPost()
        val root = json.jsonObject
        dto.id = getFirstInt(root, 0, "post_id", "id")
        dto.post_type = VKApiPost.Type.parse(optString(root, "post_type"))
        if (hasObject(root, "donut")) {
            val donut = root["donut"]?.jsonObject
            if (optBoolean(donut, "is_donut")) {
                dto.post_type = DONUT
            }
        }
        dto.owner_id = getFirstLong(root, 0, "owner_id", "to_id", "source_id")
        dto.from_id = optLong(root, "from_id")
        if (dto.from_id == 0L) {
            // "copy_history": [
            // {
            //     ... this post has been removed ...
            //     "id": 1032,
            //     "owner_id": 216143660,
            //     "from_id": 0,
            //     "date": 0,
            //     "post_type": "post",
            dto.from_id = dto.owner_id
        }
        dto.date = optLong(root, "date")
        dto.text = optString(root, "text")
        if (hasObject(root, "copyright")) {
            val cop = root["copyright"]?.jsonObject
            val name = optString(cop, "name")
            val link = optString(cop, "link")
            name.nonNullNoEmpty {
                dto.copyright = VKApiPost.Copyright(it, link)
            }
        }
        dto.reply_owner_id = optLong(root, "reply_owner_id", 0)
        if (dto.reply_owner_id == 0L) {
            // for replies from newsfeed.search
            // но не помешало бы понять какого хе...а!!!
            dto.reply_owner_id = dto.owner_id
        }
        dto.reply_post_id = optInt(root, "reply_post_id", 0)
        if (dto.reply_post_id == 0) {
            // for replies from newsfeed.search
            // но не помешало бы понять какого хе...а (1)!!!
            dto.reply_post_id = optInt(root, "post_id")
        }
        dto.friends_only = optBoolean(root, "friends_only")
        if (hasObject(root, "comments")) {
            dto.comments = root["comments"]?.let {
                kJson.decodeFromJsonElement(CommentsDto.serializer(), it)
            }
        }
        if (hasObject(root, "likes")) {
            val likes = root["likes"]?.jsonObject
            dto.likes_count = optInt(likes, "count")
            dto.user_likes = optBoolean(likes, "user_likes")
            dto.can_like = optBoolean(likes, "can_like")
            dto.can_publish = optBoolean(likes, "can_publish")
        }
        if (hasObject(root, "reposts")) {
            val reposts = root["reposts"]?.jsonObject
            dto.reposts_count = optInt(reposts, "count")
            dto.user_reposted = optBoolean(reposts, "user_reposted")
        }
        if (hasObject(root, "views")) {
            val views = root["views"]?.jsonObject
            dto.views = optInt(views, "count")
        }
        if (hasArray(root, "attachments")) {
            dto.attachments =
                root["attachments"]?.let {
                    kJson.decodeFromJsonElement(VKApiAttachments.serializer(), it)
                }
        }
        if (hasObject(root, "donut")) {
            val donut = root["donut"]?.jsonObject
            dto.is_donut = optBoolean(donut, "is_donut")
        } else {
            dto.is_donut = false
        }
        dto.can_edit = optBoolean(root, "can_edit")
        dto.is_favorite = optBoolean(root, "is_favorite")
        dto.signer_id = optLong(root, "signer_id")
        dto.created_by = optLong(root, "created_by")
        dto.can_pin = optInt(root, "can_pin") == 1
        dto.is_pinned = optBoolean(root, "is_pinned")
        if (hasArray(root, "copy_history")) {
            val copyHistoryArray = root["copy_history"]?.jsonArray
            dto.copy_history = ArrayList(copyHistoryArray?.size.orZero())
            for (i in 0 until copyHistoryArray?.size.orZero()) {
                if (!checkObject(copyHistoryArray?.get(i))) {
                    continue
                }
                val copy = copyHistoryArray?.get(i)?.jsonObject
                dto.copy_history?.add(deserialize(copy ?: continue))
            }
        } else {
            //empty list
            dto.copy_history = null
        }
        if (hasObject(root, "post_source")) {
            dto.post_source =
                root["post_source"]?.let {
                    kJson.decodeFromJsonElement(VKApiPostSource.serializer(), it)
                }
        }
        return dto
    }

    companion object {
        private val TAG = PostDtoAdapter::class.java.simpleName
    }
}