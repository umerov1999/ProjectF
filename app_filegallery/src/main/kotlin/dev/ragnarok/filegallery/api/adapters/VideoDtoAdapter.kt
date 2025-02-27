package dev.ragnarok.filegallery.api.adapters

import dev.ragnarok.filegallery.model.Video
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class VideoDtoAdapter : AbsDtoAdapter<Video>("Video") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): Video {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.jsonObject
        val dto = Video()
        dto.setId(optInt(root, "id"))
        dto.setOwnerId(optLong(root, "owner_id"))
        dto.setTitle(optString(root, "title"))
        dto.setDescription(optString(root, "description"))
        dto.setDuration(optLong(root, "duration"))
        dto.setDate(optLong(root, "date"))
        dto.setRepeat(optBoolean(root, "repeat"))
        if (hasObject(root, "files")) {
            val filesRoot = root["files"]?.jsonObject
            dto.setLink(optString(filesRoot, "mp4_720"))
        }
        if (hasArray(root, "image")) {
            val images = root["image"]?.jsonArray
            dto.setImage(images?.get(images.size - 1)?.asJsonObjectSafe?.get("url")?.asPrimitiveSafe?.content)
        }
        return dto
    }

    companion object {
        private val TAG = VideoDtoAdapter::class.simpleName.orEmpty()
    }
}
