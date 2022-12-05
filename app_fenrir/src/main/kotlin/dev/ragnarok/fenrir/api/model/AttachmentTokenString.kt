package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class AttachmentTokenString(val type: String?, val id: String?) : IAttachmentToken {
    override fun format(): String {
        return type + if (id == null || id.isEmpty()) "" else id
    }
}