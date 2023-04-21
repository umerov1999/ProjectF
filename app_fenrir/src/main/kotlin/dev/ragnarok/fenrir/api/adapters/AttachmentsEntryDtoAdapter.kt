package dev.ragnarok.fenrir.api.adapters

import dev.ragnarok.fenrir.api.adapters.AttachmentsDtoAdapter.Companion.parse
import dev.ragnarok.fenrir.api.model.VKApiAttachments
import dev.ragnarok.fenrir.api.model.VKApiNotSupported
import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import dev.ragnarok.fenrir.util.serializeble.json.JsonElement
import dev.ragnarok.fenrir.util.serializeble.json.jsonObject

class AttachmentsEntryDtoAdapter : AbsDtoAdapter<VKApiAttachments.Entry>("VKApiAttachments.Entry") {
    @Throws(Exception::class)
    override fun deserialize(
        json: JsonElement
    ): VKApiAttachments.Entry {
        if (!checkObject(json)) {
            return VKApiAttachments.Entry(
                VKApiAttachment.TYPE_NOT_SUPPORT,
                VKApiNotSupported("null", "null")
            )
        }
        val o = json.jsonObject
        val type = optString(o, "type") ?: return VKApiAttachments.Entry(
            VKApiAttachment.TYPE_NOT_SUPPORT,
            VKApiNotSupported("null", "null")
        )
        val attachment: VKApiAttachment? = try {
            parse(type, o)
        } catch (e: Exception) {
            e.printStackTrace()
            return VKApiAttachments.Entry(
                VKApiAttachment.TYPE_NOT_SUPPORT,
                VKApiNotSupported(type, "null")
            )
        }
        if (attachment != null) {
            return VKApiAttachments.Entry(type, attachment)
        }
        return VKApiAttachments.Entry(
            VKApiAttachment.TYPE_NOT_SUPPORT,
            VKApiNotSupported(type, "null")
        )
    }
}