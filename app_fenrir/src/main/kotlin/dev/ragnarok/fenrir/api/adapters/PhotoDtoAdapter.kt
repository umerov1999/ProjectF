package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.CommentsDto
import dev.ragnarok.fenrir.api.model.PhotoSizeDto
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class PhotoDtoAdapter : AbsDtoAdapter<VKApiPhoto>("VKApiPhoto") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiPhoto {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val photo = VKApiPhoto()
        val root = json.jsonObject
        photo.id = optInt(root, "id")
        photo.album_id = optInt(root, "album_id")
        photo.date = optLong(root, "date")
        photo.height = optInt(root, "height")
        photo.width = optInt(root, "width")
        photo.owner_id = optLong(root, "owner_id")
        photo.text = optString(root, "text")
        photo.access_key = optString(root, "access_key")
        if (hasObject(root, "likes")) {
            val likesRoot = root["likes"]?.jsonObject
            photo.likes = optInt(likesRoot, "count")
            photo.user_likes = optBoolean(likesRoot, "user_likes")
        }
        if (hasObject(root, "comments")) {
            photo.comments = root["comments"]?.let {
                kJson.decodeFromJsonElement(CommentsDto.serializer(), it)
            }
        }
        if (hasObject(root, "tags")) {
            val tagsRoot = root["tags"]?.jsonObject
            photo.tags = optInt(tagsRoot, "count")
        }
        if (hasObject(root, "reposts")) {
            val repostsRoot = root["reposts"]?.jsonObject
            photo.reposts = optInt(repostsRoot, "count")
        }
        photo.can_comment = optBoolean(root, "can_comment")
        photo.post_id = optInt(root, "post_id")
        if (hasArray(root, "sizes")) {
            val sizesArray = root["sizes"]?.jsonArray
            photo.sizes = ArrayList(sizesArray?.size.orZero())
            for (i in 0 until sizesArray?.size.orZero()) {
                if (!checkObject(sizesArray?.get(i))) {
                    continue
                }
                val photoSizeDto: PhotoSizeDto =
                    kJson.decodeFromJsonElement(
                        PhotoSizeDto.serializer(),
                        sizesArray?.get(i) ?: continue
                    )
                photo.sizes?.add(photoSizeDto)
                when (photoSizeDto.type) {
                    PhotoSizeDto.Type.O, PhotoSizeDto.Type.P, PhotoSizeDto.Type.Q, PhotoSizeDto.Type.R -> continue
                    else -> {
                        if (photo.width > photoSizeDto.width && photo.height > photoSizeDto.height) {
                            continue
                        }
                        photo.width = photoSizeDto.width
                        photo.height = photoSizeDto.height
                    }
                }
            }
        }
        return photo
    }

    companion object {
        private val TAG = PhotoDtoAdapter::class.java.simpleName
    }
}