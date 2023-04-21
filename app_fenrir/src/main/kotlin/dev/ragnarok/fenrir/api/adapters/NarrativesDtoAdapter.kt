package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiNarratives
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.int
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class NarrativesDtoAdapter : AbsDtoAdapter<VKApiNarratives>("VKApiNarratives") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiNarratives {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiNarratives()
        val root = json.jsonObject
        dto.id = optInt(root, "id")
        dto.owner_id = optLong(root, "owner_id")
        dto.title = optString(root, "title")
        if (hasArray(root, "story_ids")) {
            val temp = root["story_ids"]?.jsonArray
            dto.story_ids = IntArray(temp?.size.orZero()) { optInt(temp, it, 0) }
        }
        if (hasObject(root, "cover") && hasArray(root["cover"]?.jsonObject, "cropped_sizes")) {
            val images = root["cover"]?.jsonObject?.get("cropped_sizes")?.jsonArray
            for (i in 0 until images?.size.orZero()) {
                if (!checkObject(images?.get(i))) {
                    continue
                }
                if (images?.get(i)?.jsonObject?.get("width")?.asPrimitiveSafe?.int.orZero() >= 400) {
                    dto.cover = images?.get(i)?.jsonObject?.get("url")?.asPrimitiveSafe?.content
                    break
                }
            }
            if (dto.cover == null) {
                if (checkObject(images?.get(images.size - 1))) {
                    dto.cover =
                        images?.get(images.size - 1)?.jsonObject?.get("url")?.asPrimitiveSafe?.content
                }
            }
        }
        return dto
    }

    companion object {
        private val TAG = NarrativesDtoAdapter::class.java.simpleName
    }
}
