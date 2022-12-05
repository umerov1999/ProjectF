package dev.ragnarok.fenrir.model

class DraftMessage(private val id: Int, private val body: String?) {
    private var attachmentsCount = 0
    fun getAttachmentsCount(): Int {
        return attachmentsCount
    }

    fun setAttachmentsCount(attachmentsCount: Int) {
        this.attachmentsCount = attachmentsCount
    }

    fun getId(): Int {
        return id
    }

    fun getBody(): String? {
        return body
    }

    override fun toString(): String {
        return "id=" + getId() + ", body='" + getBody() + '\'' + ", count=" + attachmentsCount
    }
}