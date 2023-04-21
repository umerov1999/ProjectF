package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiPrivacy
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonArray
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class PrivacyDtoAdapter : AbsDtoAdapter<VKApiPrivacy>("VKApiPrivacy") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiPrivacy {
        if (!checkObject(json)) {
            return VKApiPrivacy("null")
        }
        val root = json.jsonObject

        // Examples
        // {"category":"only_me"}
        // {"owners":{"allowed":[13326918,26632922,31182820,50949233,113672278,138335672]}}
        val privacy = VKApiPrivacy(optString(root, "category", "only_me"))
        val owners = root["owners"]
        if (checkObject(owners)) {
            val allowed = owners.jsonObject["allowed"]
            if (checkArray(allowed)) {
                for (i in 0 until allowed.jsonArray.size) {
                    privacy.includeOwner(optLong(allowed.jsonArray, i))
                }
            }
            val excluded = owners.jsonObject["excluded"]
            if (checkArray(excluded)) {
                for (i in 0 until excluded.jsonArray.size) {
                    privacy.excludeOwner(optLong(excluded.jsonArray, i))
                }
            }
        }
        val lists = root["lists"]
        if (checkObject(lists)) {
            val allowed = lists.jsonObject["allowed"]
            if (checkArray(allowed)) {
                for (i in 0 until allowed.jsonArray.size) {
                    privacy.includeFriendsList(optLong(allowed.jsonArray, i))
                }
            }
            val excluded = lists.jsonObject["excluded"]
            if (checkArray(excluded)) {
                for (i in 0 until excluded.jsonArray.size) {
                    privacy.excludeFriendsList(optLong(excluded.jsonArray, i))
                }
            }
        }
        return privacy
    }
}