package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiReactionAsset
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class ReactionAssetDtoAdapter : AbsDtoAdapter<VKApiReactionAsset>("VKApiReactionAsset") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiReactionAsset {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val reaction = VKApiReactionAsset()
        val root = json.jsonObject

        reaction.reaction_id = optInt(root, "reaction_id")
        if (hasObject(root, "links")) {
            root["links"]?.let {
                reaction.big_animation = optString(it.jsonObject, "big_animation")
                reaction.small_animation = optString(it.jsonObject, "small_animation")
                reaction.static = optString(it.jsonObject, "static")
            }
        }
        return reaction
    }

    companion object {
        private val TAG = ReactionAssetDtoAdapter::class.java.simpleName
    }
}
