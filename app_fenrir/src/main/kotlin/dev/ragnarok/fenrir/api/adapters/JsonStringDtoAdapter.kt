package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiJsonString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

class JsonStringDtoAdapter : AbsDtoAdapter<VKApiJsonString>("VKApiJsonString") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiJsonString {
        val story = VKApiJsonString()
        if (!checkObject(json)) {
            return story
        }
        val root = json.jsonObject
        story.json_data = root.toString()
        return story
    }
}