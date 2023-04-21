package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.api.model.VKApiStory
import dev.ragnarok.fenrir.api.model.VKApiVideo
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class StoryDtoAdapter : AbsDtoAdapter<VKApiStory>("VKApiStory") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiStory {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val story = VKApiStory()
        val root = json.jsonObject
        story.id = optInt(root, "id")
        story.owner_id = optLong(root, "owner_id")
        story.date = optLong(root, "owner_id")
        story.expires_at = optLong(root, "expires_at")
        story.is_expired = optBoolean(root, "is_expired")
        story.is_ads = optBoolean(root, "is_ads")
        if (hasObject(root, "photo")) {
            story.photo = root["photo"]?.let {
                kJson.decodeFromJsonElement(VKApiPhoto.serializer(), it)
            }
        }
        if (hasObject(root, "video")) {
            story.video = root["video"]?.let {
                kJson.decodeFromJsonElement(VKApiVideo.serializer(), it)
            }
        }
        if (hasObject(root, "parent_story")) {
            story.parent_story =
                root["parent_story"]?.let {
                    kJson.decodeFromJsonElement(VKApiStory.serializer(), it)
                }
        }
        if (hasObject(root, "link")) {
            story.target_url = optString(root["link"]?.jsonObject, "url")
        }
        return story
    }

    companion object {
        private val TAG = StoryDtoAdapter::class.java.simpleName
    }
}