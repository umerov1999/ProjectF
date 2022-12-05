package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiPrivacy
import dev.ragnarok.fenrir.api.model.VKApiVideoAlbum
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.intOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive

class VideoAlbumDtoAdapter : AbsAdapter<VKApiVideoAlbum>("VKApiVideoAlbum") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiVideoAlbum {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val album = VKApiVideoAlbum()
        val root = json.asJsonObject
        album.id = optInt(root, "id")
        album.owner_id = optInt(root, "owner_id")
        album.title = optString(root, "title")
        album.count = optInt(root, "count")
        album.updated_time = optInt(root, "updated_time").toLong()
        if (hasObject(root, "privacy_view")) {
            album.privacy =
                root["privacy_view"]?.let {
                    kJson.decodeFromJsonElement(VKApiPrivacy.serializer(), it)
                }
        }
        if (hasArray(root, "image")) {
            val images = root.getAsJsonArray("image")
            for (i in 0 until images?.size.orZero()) {
                if (!checkObject(images?.get(i))) {
                    continue
                }
                if (images?.get(i)?.asJsonObject?.get("width")?.jsonPrimitive?.intOrNull.orZero() >= 800) {
                    album.image = images?.get(i)?.asJsonObject?.get("url")?.jsonPrimitive?.content
                    break
                }
            }
            if (album.image == null) {
                if (checkObject(images?.get(images.size - 1))) {
                    album.image =
                        images?.get(images.size - 1)?.asJsonObject?.get("url")?.jsonPrimitive?.content
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
        private val TAG = VideoAlbumDtoAdapter::class.java.simpleName
    }
}