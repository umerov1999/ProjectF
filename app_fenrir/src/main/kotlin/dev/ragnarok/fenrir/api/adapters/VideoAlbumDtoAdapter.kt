package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiPrivacy
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class VideoAlbumDtoAdapter : AbsDtoAdapter<VKApiVideoAlbum>("VKApiVideoAlbum") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiVideoAlbum {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val album = VKApiVideoAlbum()
        val root = json.jsonObject
        album.id = optInt(root, "id")
        album.owner_id = optLong(root, "owner_id")
        album.title = optString(root, "title")
        album.count = optInt(root, "count")
        album.updated_time = optLong(root, "updated_time")
        if (hasObject(root, "privacy_view")) {
            album.privacy =
                root["privacy_view"]?.let {
                    kJson.decodeFromJsonElement(VKApiPrivacy.serializer(), it)
                }
        }
        if (hasArray(root, "image")) {
            val images = root["image"]?.jsonArray
            for (i in 0 until images?.size.orZero()) {
                if (!checkObject(images?.get(i))) {
                    continue
                }
                if (images[i].jsonObject["width"]?.asPrimitiveSafe?.intOrNull.orZero() >= 800) {
                    album.image = images[i].jsonObject["url"]?.asPrimitiveSafe?.content
                    break
                }
            }
            if (album.image == null) {
                if (checkObject(images?.get(images.size - 1))) {
                    album.image =
                        images[images.size - 1].jsonObject["url"]?.asPrimitiveSafe?.content
                }
            }
        } else if (root.has("photo_800")) {
            album.image = optString(root, "photo_800")
        } else if (root.has("photo_320")) {
            album.image = optString(root, "photo_320")
        }
        return album
    }

    companion object {
        private val TAG = VideoAlbumDtoAdapter::class.simpleName.orEmpty()
    }
}