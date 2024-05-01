package dev.ragnarok.fenrir.model

class DraftComment(val id: Int) {
    var text: String? = null
        private set
    var attachmentsCount = 0
        private set

    fun setText(text: String?): DraftComment {
        this.text = text
        return this
    }

    fun setAttachmentsCount(attachmentsCount: Int): DraftComment {
        this.attachmentsCount = attachmentsCount
        return this
    }
}