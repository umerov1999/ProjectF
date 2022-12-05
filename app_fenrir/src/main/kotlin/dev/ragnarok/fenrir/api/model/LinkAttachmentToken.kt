package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class LinkAttachmentToken(val url: String?) : IAttachmentToken {
    override fun format(): String {
        return url ?: "null"
    }
}