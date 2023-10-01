package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.upload.Upload

class EditedMessage(val message: Message) {
    var text: String? = message.text
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
            if (text.isNullOrBlank()) {
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
