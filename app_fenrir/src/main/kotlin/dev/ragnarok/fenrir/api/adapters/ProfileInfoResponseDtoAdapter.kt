package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiProfileInfoResponse
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class ProfileInfoResponseDtoAdapter :
    AbsAdapter<VKApiProfileInfoResponse>("VKApiProfileInfoResponse") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiProfileInfoResponse {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val info = VKApiProfileInfoResponse()
        val root = json.asJsonObject
        if (root.has("name_request")) {
            info.status = 2
        } else {
            info.status = if (optInt(root, "changed", 0) == 1) 1 else 0
        }
        return info
    }

    companion object {
        private val TAG = ProfileInfoResponseDtoAdapter::class.java.simpleName
    }
}