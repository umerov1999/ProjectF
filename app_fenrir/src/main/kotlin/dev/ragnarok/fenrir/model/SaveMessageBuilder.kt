package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import java.io.File

class SaveMessageBuilder(val accountId: Long, val peerId: Long) {
    var attachments: MutableList<AbsModel>? = null
        private set
    var forwardMessages: List<Message>? = null
        private set
    var text: String? = null
        private set
    var voiceMessageFile: File? = null
        private set
    var requireEncryption = false
        private set
    var draftMessageId: Int? = null
        private set
    var payload: String? = null
        private set

    @KeyLocationPolicy
    var keyLocationPolicy: Int
        private set

    fun setDraftMessageId(draftMessageId: Int?): SaveMessageBuilder {
        this.draftMessageId = draftMessageId
        return this
    }

    fun attach(attachments: List<AbsModel>?): SaveMessageBuilder {
        if (attachments != null) {
            prepareAttachments(attachments.size).addAll(attachments)
        }
        return this
    }

    private fun prepareAttachments(initialSize: Int): MutableList<AbsModel> {
        if (attachments == null) {
            attachments = ArrayList(initialSize)
        }
        return attachments!!
    }

    fun attach(attachment: AbsModel): SaveMessageBuilder {
        prepareAttachments(1).add(attachment)
        return this
    }

    fun setForwardMessages(forwardMessages: List<Message>?): SaveMessageBuilder {
        this.forwardMessages = forwardMessages
        return this
    }

    fun setText(text: String?): SaveMessageBuilder {
        this.text = text
        return this
    }

    fun setVoiceMessageFile(voiceMessageFile: File?): SaveMessageBuilder {
        this.voiceMessageFile = voiceMessageFile
        return this
    }

    fun setRequireEncryption(requireEncryption: Boolean): SaveMessageBuilder {
        this.requireEncryption = requireEncryption
        return this
    }

    fun setKeyLocationPolicy(keyLocationPolicy: Int): SaveMessageBuilder {
        this.keyLocationPolicy = keyLocationPolicy
        return this
    }

    fun setPayload(payload: String?): SaveMessageBuilder {
        this.payload = payload
        return this
    }

    init {
        keyLocationPolicy = KeyLocationPolicy.PERSIST
    }
}