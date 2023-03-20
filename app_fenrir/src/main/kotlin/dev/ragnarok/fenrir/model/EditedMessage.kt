package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.upload.Upload

class EditedMessage(val message: Message) {

    var body: String? = message.body
    val attachments: MutableList<AttachmentEntry>

    init {
        val orig = message.attachments?.toList() ?: ArrayList()

        attachments = ArrayList()

        for (model in orig) {
            attachments.add(AttachmentEntry(true, model))
        }

        message.fwd?.run {
            attachments.add(AttachmentEntry(true, FwdMessages(this)))
        }
    }

    val canSave: Boolean
        get() {
            if (body.isNullOrBlank()) {
                for (entry in attachments) {
                    if (entry.attachment is Upload) {
                        continue
                    }
                    return true
                }
                return false
            } else {
                return true
            }
        }
}
