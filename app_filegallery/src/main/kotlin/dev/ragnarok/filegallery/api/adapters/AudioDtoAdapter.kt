package dev.ragnarok.filegallery.api.adapters

import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.util.serializeble.json.JsonElement
import dev.ragnarok.filegallery.util.serializeble.json.jsonObject

class AudioDtoAdapter : AbsDtoAdapter<Audio>("Audio") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): Audio {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = Audio()
        val root = json.jsonObject
        dto.setId(optInt(root, "id"))
        dto.setOwnerId(optLong(root, "owner_id"))
        dto.setArtist(optString(root, "artist"))
        dto.setTitle(optString(root, "title"))
        dto.setDuration(optInt(root, "duration"))
        dto.setUrl(optString(root, "url"))
        if (hasObject(root, "album")) {
            var thmb = root["album"]?.jsonObject
            if (hasObject(thmb, "thumb")) {
                thmb = thmb["thumb"]?.jsonObject
                if (thmb.has("photo_600")) {
                    dto.setThumb_image(optString(thmb, "photo_600"))
                }
            }
        }
        dto.updateDownloadIndicator()
        return dto
    }

    companion object {
        private val TAG = AudioDtoAdapter::class.java.simpleName
    }
}
