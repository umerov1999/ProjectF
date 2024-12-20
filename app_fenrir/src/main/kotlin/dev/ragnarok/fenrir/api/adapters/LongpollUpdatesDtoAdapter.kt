package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.longpoll.AbsLongpollEvent
import dev.ragnarok.fenrir.api.model.longpoll.VKApiLongpollUpdates
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.Logger
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class LongpollUpdatesDtoAdapter : AbsDtoAdapter<VKApiLongpollUpdates>("VkApiLongpollUpdates") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiLongpollUpdates {
        val updates = VKApiLongpollUpdates()
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.jsonObject
        updates.failed = optInt(root, "failed")
        updates.ts = optLong(root, "ts")
        val array = root["updates"]
        if (checkArray(array)) {
            for (i in 0 until array.jsonArray.size) {
                val updateArray = array.jsonArray[i].asJsonArraySafe
                val event: AbsLongpollEvent? =
                    updateArray?.let {
                        kJson.decodeFromJsonElement(
                            AbsLongpollEvent.serializer(),
                            it
                        )
                    }
                if (event != null) {
                    updates.putUpdate(event)
                } else {
                    Logger.d(TAG, "Unhandled Longpoll event: array: $updateArray")
                }
            }
        }
        return updates
    }

    companion object {
        private val TAG = LongpollUpdatesDtoAdapter::class.simpleName.orEmpty()
    }
}