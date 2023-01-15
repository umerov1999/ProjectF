package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.Serializable

@Serializable
class VKApiGiftItem : VKApiAttachment {
    var id = 0
    var thumb_256: String? = null
    var thumb_96: String? = null
    var thumb_48: String? = null
    override fun getType(): String {
        return VKApiAttachment.TYPE_GIFT
    }
}