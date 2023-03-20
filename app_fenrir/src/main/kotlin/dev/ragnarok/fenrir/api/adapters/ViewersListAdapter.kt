package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiUser
import dev.ragnarok.fenrir.api.model.response.ViewersListResponse
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.requireNonNull
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class ViewersListAdapter : AbsAdapter<ViewersListResponse>("ViewersListAdapter") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): ViewersListResponse {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val response = ViewersListResponse()
        val root = json.asJsonObject
        response.count = optInt(root, "count")
        if (hasArray(root, "items")) {
            val itemsArray = root.getAsJsonArray("items")
            response.ownersWithLikes = ArrayList(itemsArray?.size.orZero())
            for (i in 0 until itemsArray?.size.orZero()) {
                if (!checkObject(itemsArray?.get(i))) {
                    continue
                }
                val itemRoot = itemsArray?.get(i)?.asJsonObject
                if (!hasObject(itemRoot, "user")) {
                    continue
                }
                val isLiked = optBoolean(itemRoot, "is_liked")
                val userRoot = itemRoot["user"]
                val owner = userRoot?.let {
                    kJson.decodeFromJsonElement(VKApiUser.serializer(), it)
                }
                owner.requireNonNull {
                    response.ownersWithLikes?.add(Pair(it, isLiked))
                }
            }
        }
        return response
    }

    companion object {
        private val TAG = ViewersListAdapter::class.java.simpleName
    }
}