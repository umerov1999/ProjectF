package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.database.SchoolClazzDto
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.intOrNull
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive

class SchoolClazzDtoAdapter : AbsDtoAdapter<SchoolClazzDto>("SchoolClazzDto") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): SchoolClazzDto {
        if (!checkArray(json)) {
            throw Exception("$TAG error parse object")
        }
        val dto = SchoolClazzDto()
        val root = json.jsonArray
        dto.id = optInt(root, 0)
        if (checkPrimitive(root[1])) {
            val second = root[1].jsonPrimitive
            if (second.isString) {
                dto.title = second.content
            } else {
                dto.title = second.intOrNull?.toString()
            }
        }
        return dto
    }

    companion object {
        private val TAG = SchoolClazzDtoAdapter::class.java.simpleName
    }
}