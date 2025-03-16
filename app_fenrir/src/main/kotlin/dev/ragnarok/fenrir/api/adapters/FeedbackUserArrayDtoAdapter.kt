package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.feedback.UserArray
import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class FeedbackUserArrayDtoAdapter : AbsDtoAdapter<UserArray>("UserArray") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): UserArray {
        val root = json.jsonObject
        val dto = UserArray()
        dto.count = optInt(root, "count")
        if (hasArray(root, "items")) {
            val array = root["items"]?.jsonArray
            dto.ids = LongArray(array?.size.orZero())
            for (i in 0 until array?.size.orZero()) {
                if (!checkObject(array?.get(i))) {
                    continue
                }
                dto.ids?.set(i, optLong(array[i].jsonObject, "from_id", 0))
            }
        } else {
            dto.ids = LongArray(0)
        }
        return dto
    }
}