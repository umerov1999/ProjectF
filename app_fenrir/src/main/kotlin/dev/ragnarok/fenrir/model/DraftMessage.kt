package dev.ragnarok.fenrir.model

class DraftMessage(private val id: Int, private val text: String?) {
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

    fun getText(): String? {
        return text
    }

    override fun toString(): String {
        return "id=" + getId() + ", text='" + getText() + '\'' + ", count=" + attachmentsCount
    }
}