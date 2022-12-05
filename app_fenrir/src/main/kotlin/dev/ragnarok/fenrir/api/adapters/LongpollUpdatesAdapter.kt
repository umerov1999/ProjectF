package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.longpoll.AbsLongpollEvent
import dev.ragnarok.fenrir.api.model.longpoll.VkApiLongpollUpdates
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class LongpollUpdatesAdapter : AbsAdapter<VkApiLongpollUpdates>("VkApiLongpollUpdates") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VkApiLongpollUpdates {
        val updates = VkApiLongpollUpdates()
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val root = json.asJsonObject
        updates.failed = optInt(root, "failed")
        updates.ts = optLong(root, "ts")
        val array = root["updates"]
        if (checkArray(array)) {
            for (i in 0 until array.asJsonArray.size) {
                val updateArray = array.asJsonArraySafe?.get(i)?.asJsonArraySafe
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
        private val TAG = LongpollUpdatesAdapter::class.java.simpleName
    }
}