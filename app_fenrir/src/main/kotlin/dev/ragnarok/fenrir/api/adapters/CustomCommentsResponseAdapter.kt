package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.response.CustomCommentsResponse
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.JsonPrimitive
import dev.ragnarok.fenrir.util.serializeble.json.intOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive

class CustomCommentsResponseAdapter : AbsAdapter<CustomCommentsResponse>("CustomCommentsResponse") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): CustomCommentsResponse {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val response = CustomCommentsResponse()
        val root = json.asJsonObject
        val main = root["main"]
        if (checkObject(main)) {
            response.main =
                kJson.decodeFromJsonElement(CustomCommentsResponse.Main.serializer(), main)
        } // "main": false (if has execute errors)
        if (root.has("first_id")) {
            val firstIdJson = root["first_id"]
            response.firstId =
                if (firstIdJson is JsonPrimitive) firstIdJson.jsonPrimitive.intOrNull else null
        }
        if (root.has("last_id")) {
            val lastIdJson = root["last_id"]
            response.lastId =
                if (lastIdJson is JsonPrimitive) lastIdJson.jsonPrimitive.intOrNull else null
        }
        if (root.has("admin_level")) {
            val adminLevelJson = root["admin_level"]
            response.admin_level =
                if (adminLevelJson is JsonPrimitive) adminLevelJson.jsonPrimitive.intOrNull else null
        }
        return response
    }

    companion object {
        private val TAG = CustomCommentsResponseAdapter::class.java.simpleName
    }
}