package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

@Serializable
class VKApiNotSupported(val attachmentType: String, val bodyJson: String?) : VKApiAttachment {
    override fun getType(): String {
        return VKApiAttachment.TYPE_NOT_SUPPORT
    }
}