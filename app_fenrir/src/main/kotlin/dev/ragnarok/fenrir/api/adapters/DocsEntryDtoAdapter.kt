package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiDoc
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

class DocsEntryDtoAdapter : AbsAdapter<VKApiDoc.Entry>("VKApiDoc.Entry") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiDoc.Entry {
        val o = json.asJsonObject
        val type = optString(o, "type") ?: throw NullPointerException("VKApiDoc.Entry type")
        val entry: VKApiDoc.Entry
        val pp: VKApiDoc =
            kJson.decodeFromJsonElement(VKApiDoc.serializer(), o[type]!!)
        entry = VKApiDoc.Entry(type, pp)
        return entry
    }
}