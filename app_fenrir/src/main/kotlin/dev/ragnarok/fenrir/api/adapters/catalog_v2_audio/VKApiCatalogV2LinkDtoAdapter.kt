package dev.ragnarok.fenrir.api.adapters.catalog_v2_audio

import dev.ragnarok.fenrir.api.adapters.AbsDtoAdapter
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2Link
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class VKApiCatalogV2LinkDtoAdapter : AbsDtoAdapter<VKApiCatalogV2Link>("VKApiCatalogV2Link") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiCatalogV2Link {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiCatalogV2Link()
        val root = json.jsonObject
        dto.id = optString(root, "id")
        dto.url = optString(root, "url")
        dto.title = optString(root, "title")
        dto.subtitle = optString(root, "subtitle")
        if (hasArray(root, "image")) {
            val arr = root["image"]?.jsonArray
            var max_res = 0
            for (i in arr.orEmpty()) {
                if (!checkObject(i)) {
                    continue
                }
                val res = i.jsonObject
                val curr_res = optInt(res, "height") * optInt(res, "width")
                if (curr_res in (max_res + 1)..40000) {
                    max_res = curr_res
                    dto.preview_photo = optString(res, "url")
                }
            }
            if (dto.preview_photo.isNullOrEmpty() && arr?.size.orZero() > 0) {
                val i = arr?.get(arr.size - 1)
                if (checkObject(i)) {
                    dto.preview_photo = optString(i, "url")
                }
            }
        }
        return dto
    }

    companion object {
        private val TAG = VKApiCatalogV2LinkDtoAdapter::class.java.simpleName
    }
}
