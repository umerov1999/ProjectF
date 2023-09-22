package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.crypt.KeyLocationPolicy
import java.io.File

class SaveMessageBuilder(private val accountId: Long, private val peerId: Long) {
    private var attachments: MutableList<AbsModel>? = null
    private var forwardMessages: List<Message>? = null
    private var body: String? = null
    private var voiceMessageFile: File? = null
    private var requireEncryption = false
    private var draftMessageId: Int? = null
    private var payload: String? = null

    @KeyLocationPolicy
    private var keyLocationPolicy: Int
    fun getDraftMessageId(): Int? {
        return draftMessageId
    }

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

    fun getAccountId(): Long {
        return accountId
    }

    fun getPeerId(): Long {
        return peerId
    }

    fun getAttachments(): List<AbsModel>? {
        return attachments
    }

    fun getForwardMessages(): List<Message>? {
        return forwardMessages
    }

    fun setForwardMessages(forwardMessages: List<Message>?): SaveMessageBuilder {
        this.forwardMessages = forwardMessages
        return this
    }

    fun getBody(): String? {
        return body
    }

    fun setBody(body: String?): SaveMessageBuilder {
        this.body = body
        return this
    }

    fun getVoiceMessageFile(): File? {
        return voiceMessageFile
    }

    fun setVoiceMessageFile(voiceMessageFile: File?): SaveMessageBuilder {
        this.voiceMessageFile = voiceMessageFile
        return this
    }

    fun isRequireEncryption(): Boolean {
        return requireEncryption
    }

    fun setRequireEncryption(requireEncryption: Boolean): SaveMessageBuilder {
        this.requireEncryption = requireEncryption
        return this
    }

    @KeyLocationPolicy
    fun getKeyLocationPolicy(): Int {
        return keyLocationPolicy
    }

    fun setKeyLocationPolicy(keyLocationPolicy: Int): SaveMessageBuilder {
        this.keyLocationPolicy = keyLocationPolicy
        return this
    }

    fun getPayload(): String? {
        return payload
    }

    fun setPayload(payload: String?): SaveMessageBuilder {
        this.payload = payload
        return this
    }

    init {
        keyLocationPolicy = KeyLocationPolicy.PERSIST
    }
}