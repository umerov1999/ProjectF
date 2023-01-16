package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.adapters.ArticleDtoAdapter
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

@Serializable(with = ArticleDtoAdapter::class)
class VKApiArticle : VKApiAttachment {
    var id = 0
    var owner_id = 0L
    var owner_name: String? = null
    var url: String? = null
    var title: String? = null
    var subtitle: String? = null
    var photo: VKApiPhoto? = null
    var is_favorite = false
    var access_key: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_ARTICLE
    }
}