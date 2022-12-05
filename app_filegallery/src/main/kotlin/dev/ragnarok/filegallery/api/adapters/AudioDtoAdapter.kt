package dev.ragnarok.filegallery.api.adapters

import dev.ragnarok.filegallery.model.Audio
import dev.ragnarok.filegallery.util.serializeble.json.JsonElement

class AudioDtoAdapter : AbsAdapter<Audio>("Audio") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): Audio {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = Audio()
        val root = json.asJsonObject
        dto.setId(optInt(root, "id"))
        dto.setOwnerId(optInt(root, "owner_id"))
        dto.setArtist(optString(root, "artist"))
        dto.setTitle(optString(root, "title"))
        dto.setDuration(optInt(root, "duration"))
        dto.setUrl(optString(root, "url"))
        if (hasObject(root, "album")) {
            var thmb = root.getAsJsonObject("album")
            if (hasObject(thmb, "thumb")) {
                thmb = thmb.getAsJsonObject("thumb")
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
