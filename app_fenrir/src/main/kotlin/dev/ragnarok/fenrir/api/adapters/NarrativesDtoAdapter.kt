package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiNarratives
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.int
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive

class NarrativesDtoAdapter : AbsAdapter<VKApiNarratives>("VKApiNarratives") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiNarratives {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiNarratives()
        val root = json.asJsonObject
        dto.id = optInt(root, "id")
        dto.owner_id = optInt(root, "owner_id")
        dto.title = optString(root, "title")
        if (hasArray(root, "story_ids")) {
            val temp = root.getAsJsonArray("story_ids")
            dto.story_ids = IntArray(temp?.size.orZero()) { optInt(temp, it, 0) }
        }
        if (hasObject(root, "cover") && hasArray(root.getAsJsonObject("cover"), "cropped_sizes")) {
            val images = root.getAsJsonObject("cover").getAsJsonArray("cropped_sizes")
            for (i in 0 until images?.size.orZero()) {
                if (!checkObject(images?.get(i))) {
                    continue
                }
                if (images?.get(i)?.asJsonObject?.get("width")?.jsonPrimitive?.int.orZero() >= 400) {
                    dto.cover = images?.get(i)?.asJsonObject?.get("url")?.jsonPrimitive?.content
                    break
                }
            }
            if (dto.cover == null) {
                if (checkObject(images?.get(images.size - 1))) {
                    dto.cover =
                        images?.get(images.size - 1)?.asJsonObject?.get("url")?.jsonPrimitive?.content
                }
            }
        }
        return dto
    }

    companion object {
        private val TAG = NarrativesDtoAdapter::class.java.simpleName
    }
}
