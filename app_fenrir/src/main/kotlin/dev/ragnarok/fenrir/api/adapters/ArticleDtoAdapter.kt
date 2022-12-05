package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.model.VKApiArticle
import dev.ragnarok.fenrir.api.model.VKApiPhoto
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement

import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class ArticleDtoAdapter : AbsAdapter<VKApiArticle>("VKApiArticle") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiArticle {
        if (!checkObject(json)) {
            throw Exception("$TAG error parse object")
        }
        val article = VKApiArticle()
        val root = json.jsonObject
        article.id = optInt(root, "id")
        article.owner_id = optInt(root, "owner_id")
        article.owner_name = optString(root, "owner_name")
        article.title = optString(root, "title")
        article.subtitle = optString(root, "subtitle")
        article.access_key = optString(root, "access_key")
        article.is_favorite = optBoolean(root, "is_favorite")
        if (hasObject(root, "photo")) {
            article.photo = root["photo"]?.let {
                kJson.decodeFromJsonElement(VKApiPhoto.serializer(), it)
            }
        }
        if (root.containsKey("view_url")) {
            article.url = optString(root, "view_url")
        } else if (root.containsKey("url")) {
            article.url = optString(root, "url")
        }
        return article
    }

    companion object {
        private val TAG = ArticleDtoAdapter::class.java.simpleName
    }
}