package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.orZero
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiWallReply : VKApiAttachment {
    @SerialName("id")
    var id = 0

    @SerialName("from_id")
    var from_id = 0

    @SerialName("post_id")
    var post_id = 0

    @SerialName("owner_id")
    var owner_id = 0

    @SerialName("text")
    var text: String? = null

    @SerialName("attachments")
    var attachments: VKApiAttachments? = null
    val attachmentsCount: Int
        get() = attachments?.size().orZero()

    fun hasAttachments(): Boolean {
        return attachmentsCount > 0
    }

    override fun getType(): String {
        return VKApiAttachment.TYPE_WALL_REPLY
    }
}