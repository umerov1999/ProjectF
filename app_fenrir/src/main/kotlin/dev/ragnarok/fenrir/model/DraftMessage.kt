package dev.ragnarok.fenrir.model

class DraftMessage(val id: Int, val text: String?) {
    var attachmentsCount = 0
        private set

    fun setAttachmentsCount(attachmentsCount: Int) {
        this.attachmentsCount = attachmentsCount
    }

    override fun toString(): String {
        return "id=$id, text='$text', count=$attachmentsCount"
    }
}