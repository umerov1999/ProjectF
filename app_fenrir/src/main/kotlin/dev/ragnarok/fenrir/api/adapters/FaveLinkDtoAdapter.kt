package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.FaveLinkDto
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.kJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class FaveLinkDtoAdapter : AbsDtoAdapter<FaveLinkDto>("FaveLinkDto") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): FaveLinkDto {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val link = FaveLinkDto()
        var root: JsonObject? = json.jsonObject
        if (!hasObject(root, "link")) return link
        root = root["link"]?.jsonObject
        link.id = optString(root, "id")
        link.description = optString(root, "description")
        if (hasObject(root, "photo")) {
            link.photo = root["photo"]?.let {
                kJson.decodeFromJsonElement(VKApiPhoto.serializer(), it)
            }
        }
        link.title = optString(root, "title")
        link.url = optString(root, "url")
        return link
    }

    companion object {
        private val TAG = FaveLinkDtoAdapter::class.simpleName.orEmpty()
    }
}