package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.CommentsDto
import dev.ragnarok.fenrir.api.model.VKApiPrivacy
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class VideoDtoAdapter : AbsAdapter<VKApiVideo>("VKApiVideo") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiVideo {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.asJsonObject
        val dto = VKApiVideo()
        dto.id = optInt(root, "id")
        dto.owner_id = optLong(root, "owner_id")
        dto.title = optString(root, "title")
        dto.description = optString(root, "description")
        dto.duration = optLong(root, "duration")
        dto.link = optString(root, "link")
        dto.date = optLong(root, "date")
        dto.adding_date = optLong(root, "adding_date")
        dto.views = optInt(root, "views")
        if (hasObject(root, "comments")) {
            //for example, newsfeed.getComment
            dto.comments = root["comments"]?.let {
                kJson.decodeFromJsonElement(CommentsDto.serializer(), it)
            }
        } else {
            // video.get
            dto.comments = CommentsDto()
            dto.comments?.count = optInt(root, "comments", 0)
        }
        dto.player = optString(root, "player")
        dto.access_key = optString(root, "access_key")
        dto.album_id = optInt(root, "album_id")
        if (hasObject(root, "likes")) {
            val likesRoot = root.getAsJsonObject("likes")
            dto.likes = optInt(likesRoot, "count")
            dto.user_likes = optBoolean(likesRoot, "user_likes")
        }
        dto.can_comment = optBoolean(root, "can_comment")
        dto.can_repost = optBoolean(root, "can_repost")
        dto.repeat = optBoolean(root, "repeat")
        if (hasObject(root, "privacy_view")) {
            dto.privacy_view =
                root["privacy_view"]?.let {
                    kJson.decodeFromJsonElement(VKApiPrivacy.serializer(), it)
                }
        }
        if (hasObject(root, "privacy_comment")) {
            dto.privacy_comment =
                root["privacy_comment"]?.let {
                    kJson.decodeFromJsonElement(VKApiPrivacy.serializer(), it)
                }
        }
        if (hasObject(root, "files")) {
            val filesRoot = root.getAsJsonObject("files")
            dto.mp4_240 = optString(filesRoot, "mp4_240")
            dto.mp4_360 = optString(filesRoot, "mp4_360")
            dto.mp4_480 = optString(filesRoot, "mp4_480")
            dto.mp4_720 = optString(filesRoot, "mp4_720")
            dto.mp4_1080 = optString(filesRoot, "mp4_1080")
            dto.mp4_1440 = optString(filesRoot, "mp4_1440")
            dto.mp4_2160 = optString(filesRoot, "mp4_2160")
            dto.external = optString(filesRoot, "external")
            dto.hls = optString(filesRoot, "hls")
            dto.live = optString(filesRoot, "live")
        }
        val sz =
            if (dto.external.nonNullNoEmpty() && dto.external?.contains("youtube") == true) 320 else 800
        if (hasArray(root, "image")) {
            val images = root.getAsJsonArray("image")
            if (images?.size.orZero() > 0) {
                for (i in 0 until images?.size.orZero()) {
                    if (optInt(images?.get(i)?.asJsonObject, "width") >= sz) {
                        dto.image = optString(images?.get(i)?.asJsonObject, "url")
                        break
                    }
                }
                if (dto.image == null) dto.image =
                    optString(images?.get(images.size - 1)?.asJsonObject, "url")
            }
        } else if (dto.image == null && hasArray(root, "first_frame")) {
            val images = root.getAsJsonArray("first_frame")
            if (images?.size.orZero() > 0) {
                for (i in 0 until images?.size.orZero()) {
                    if (optInt(images?.get(i)?.asJsonObject, "width") >= 800) {
                        dto.image = optString(images?.get(i)?.asJsonObject, "url")
                        break
                    }
                }
                if (dto.image == null) dto.image =
                    optString(images?.get(images.size - 1)?.asJsonObject, "url")
            }
        } else if (dto.image == null) {
            if (root.has("photo_800")) {
                dto.image = optString(root, "photo_800")
            } else if (root.has("photo_320")) {
                dto.image = optString(root, "photo_320")
            }
        }
        dto.platform = optString(root, "platform")
        dto.can_edit = optBoolean(root, "can_edit")
        dto.can_add = optBoolean(root, "can_add")
        dto.is_private = optBoolean(root, "is_private")
        dto.is_favorite = optBoolean(root, "is_favorite")
        return dto
    }

    companion object {
        private val TAG = VideoDtoAdapter::class.java.simpleName
    }
}
