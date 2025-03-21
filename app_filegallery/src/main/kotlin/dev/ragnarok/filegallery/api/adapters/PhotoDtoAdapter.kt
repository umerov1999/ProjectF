package dev.ragnarok.filegallery.api.adapters

import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.orZero
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class PhotoDtoAdapter : AbsDtoAdapter<Photo>("Photo") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): Photo {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val photo = Photo()
        val root = json.jsonObject
        photo.setId(optInt(root, "id"))
        photo.setDate(optLong(root, "date"))
        photo.setOwnerId(optLong(root, "owner_id"))
        photo.setText(optString(root, "text"))
        if (hasArray(root, "sizes")) {
            val sizesArray = root["sizes"]?.jsonArray
            for (i in 0 until sizesArray?.size.orZero()) {
                if (!checkObject(sizesArray?.get(i))) {
                    continue
                }
                val p = sizesArray[i].jsonObject
                if (optString(p, "type").equals("w")) {
                    photo.setPhoto_url(optString(p, "url"))
                } else if (optString(p, "type").equals("s")) {
                    photo.setPreview_url(optString(p, "url"))
                }
            }
        }
        return photo
    }

    companion object {
        private val TAG = PhotoDtoAdapter::class.simpleName.orEmpty()
    }
}
