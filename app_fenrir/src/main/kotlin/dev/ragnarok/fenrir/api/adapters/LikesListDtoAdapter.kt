package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiOwner
import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.LikesListResponse
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class LikesListDtoAdapter : AbsDtoAdapter<LikesListResponse>("LikesListResponse") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): LikesListResponse {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val response = LikesListResponse()
        val root = json.jsonObject
        response.count = optInt(root, "count")
        if (hasArray(root, "items")) {
            val itemsArray = root["items"]?.jsonArray
            response.owners = ArrayList(itemsArray?.size.orZero())
            for (i in 0 until itemsArray?.size.orZero()) {
                if (!checkObject(itemsArray?.get(i))) {
                    continue
                }
                val itemRoot = itemsArray?.get(i)?.jsonObject
                val type = optString(itemRoot, "type")
                var owner: VKApiOwner? = null
                if ("profile" == type || "user" == type) {
                    owner = itemRoot?.let {
                        kJson.decodeFromJsonElement(VKApiUser.serializer(), it)
                    }
                } else if ("group" == type || "page" == type) {
                    owner = itemRoot?.let {
                        kJson.decodeFromJsonElement(VKApiCommunity.serializer(), it)
                    }
                }
                owner.requireNonNull {
                    response.owners?.add(it)
                }
            }
        }
        return response
    }

    companion object {
        private val TAG = LikesListDtoAdapter::class.java.simpleName
    }
}