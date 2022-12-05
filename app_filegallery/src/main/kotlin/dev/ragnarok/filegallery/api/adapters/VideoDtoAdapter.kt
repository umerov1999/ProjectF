package dev.ragnarok.filegallery.api.adapters

import dev.ragnarok.filegallery.model.Video
import dev.ragnarok.filegallery.util.serializeble.json.JsonElement
import dev.ragnarok.filegallery.util.serializeble.json.jsonPrimitive

class VideoDtoAdapter : AbsAdapter<Video>("Video") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): Video {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.asJsonObject
        val dto = Video()
        dto.setId(optInt(root, "id"))
        dto.setOwnerId(optInt(root, "owner_id"))
        dto.setTitle(optString(root, "title"))
        dto.setDescription(optString(root, "description"))
        dto.setDuration(optInt(root, "duration"))
        dto.setDate(optLong(root, "date"))
        dto.setRepeat(optBoolean(root, "repeat"))
        if (hasObject(root, "files")) {
            val filesRoot = root.getAsJsonObject("files")
            dto.setLink(optString(filesRoot, "mp4_720"))
        }
        if (hasArray(root, "image")) {
            val images = root.getAsJsonArray("image")
            dto.setImage(images?.get(images.size - 1)?.asJsonObject?.get("url")?.jsonPrimitive?.content)
        }
        return dto
    }

    companion object {
        private val TAG = VideoDtoAdapter::class.java.simpleName
    }
}
