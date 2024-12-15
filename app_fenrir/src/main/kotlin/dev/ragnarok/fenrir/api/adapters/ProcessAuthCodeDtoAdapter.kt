package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiProcessAuthCode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

class ProcessAuthCodeDtoAdapter : AbsDtoAdapter<VKApiProcessAuthCode>("VKApiProcessAuthCode") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiProcessAuthCode {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val ret = VKApiProcessAuthCode()
        val root = json.jsonObject
        ret.status = optInt(root, "status")
        if (hasObject(root, "auth_info") && hasObject(
                root["auth_info"]?.jsonObject,
                "device_info"
            )
        ) {
            root["auth_info"]?.jsonObject?.let {
                ret.auth_id = optString(it, "auth_id")
            }
            root["auth_info"]?.jsonObject?.get("device_info")?.jsonObject?.let {
                ret.browser_name = optString(it, "browser_name")
                ret.ip = optString(it, "ip")
                ret.location = optString(it, "location")
            }
        }
        return ret
    }

    companion object {
        private val TAG = ProcessAuthCodeDtoAdapter::class.simpleName.orEmpty()
    }
}
