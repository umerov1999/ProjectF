package dev.ragnarok.fenrir.model

class DraftComment(private val id: Int) {
    private var body: String? = null
    private var attachmentsCount = 0
    fun getId(): Int {
        return id
    }

    fun getBody(): String? {
        return body
    }

    fun setBody(body: String?): DraftComment {
        this.body = body
        return this
    }

    fun getAttachmentsCount(): Int {
        return attachmentsCount
    }

    fun setAttachmentsCount(attachmentsCount: Int): DraftComment {
        this.attachmentsCount = attachmentsCount
        return this
    }
}