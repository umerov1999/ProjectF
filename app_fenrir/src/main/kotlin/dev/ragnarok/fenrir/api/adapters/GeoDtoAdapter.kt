package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiGeo
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class GeoDtoAdapter : AbsDtoAdapter<VKApiGeo>("VKApiGeo") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiGeo {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val geo = VKApiGeo()
        val root = json.jsonObject
        val coordinates = optString(root, "coordinates")
        try {
            val tmp = coordinates?.split(" ")
            if (tmp.orEmpty().size >= 2) {
                geo.latitude = tmp?.get(0)
                geo.longitude = tmp?.get(1)
            }
        } catch (ignored: Exception) {
        }
        if (hasObject(root, "place")) {
            root["place"]?.let {
                geo.title = optString(it.jsonObject, "title")
                geo.address = optString(it.jsonObject, "address")
                geo.country = optInt(it.jsonObject, "country")
                geo.id = optInt(it.jsonObject, "id")
                if (geo.latitude.isNullOrEmpty()) {
                    try {
                        geo.latitude = optDouble(it.jsonObject, "latitude").toString()
                        geo.longitude = optDouble(it.jsonObject, "longitude").toString()
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
        return geo
    }

    companion object {
        private val TAG = GeoDtoAdapter::class.java.simpleName
    }
}
