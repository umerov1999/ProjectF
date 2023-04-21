package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiMarket
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class MarketDtoAdapter : AbsDtoAdapter<VKApiMarket>("VKApiMarket") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiMarket {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = VKApiMarket()
        val root = json.jsonObject
        dto.id = optInt(root, "id")
        dto.owner_id = optLong(root, "owner_id")
        dto.weight = optInt(root, "weight")
        dto.availability = optInt(root, "availability")
        dto.date = optLong(root, "date")
        dto.title = optString(root, "title")
        dto.description = optString(root, "description")
        dto.thumb_photo = optString(root, "thumb_photo")
        dto.sku = optString(root, "sku")
        dto.access_key = optString(root, "access_key")
        dto.is_favorite = optBoolean(root, "is_favorite")
        if (hasObject(root, "dimensions")) {
            val dimensions = root["dimensions"]?.jsonObject
            dto.dimensions = optInt(dimensions, "length").toString() + "x" + optInt(
                dimensions,
                "width"
            ) + "x" + optInt(dimensions, "height") + " mm"
        }
        if (hasObject(root, "price")) {
            val price = root["price"]?.jsonObject
            dto.price = optString(price, "text")
        }
        if (hasArray(root, "photos")) {
            dto.photos = ArrayList()
            val temp = root["photos"]?.jsonArray
            for (i in temp.orEmpty()) {
                if (!checkObject(i)) {
                    continue
                }
                dto.photos?.add(kJson.decodeFromJsonElement(VKApiPhoto.serializer(), i))
            }
        }
        return dto
    }

    companion object {
        private val TAG = MarketDtoAdapter::class.java.simpleName
    }
}