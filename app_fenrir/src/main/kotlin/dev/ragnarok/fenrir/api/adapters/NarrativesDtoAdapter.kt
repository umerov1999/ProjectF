package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiNarratives
import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

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
        dto.access_key = optString(root, "access_key")
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
                if (images[i].jsonObject["width"]?.asPrimitiveSafe?.int.orZero() >= 400) {
                    dto.cover = images[i].jsonObject["url"]?.asPrimitiveSafe?.content
                    break
                }
            }
            if (dto.cover == null) {
                if (checkObject(images?.get(images.size - 1))) {
                    dto.cover =
                        images[images.size - 1].jsonObject["url"]?.asPrimitiveSafe?.content
                }
            }
        }
        return dto
    }

    companion object {
        private val TAG = NarrativesDtoAdapter::class.simpleName.orEmpty()
    }
}
