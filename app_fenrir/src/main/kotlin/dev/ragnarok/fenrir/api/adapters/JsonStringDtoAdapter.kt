package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiJsonString
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class JsonStringDtoAdapter : AbsAdapter<VKApiJsonString>("VKApiJsonString") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiJsonString {
        val story = VKApiJsonString()
        if (!checkObject(json)) {
            return story
        }
        val root = json.asJsonObject
        story.json_data = root.toString()
        return story
    }
}