package dev.ragnarok.fenrir.model

class DraftComment(private val id: Int) {
    private var text: String? = null
    private var attachmentsCount = 0
    fun getId(): Int {
        return id
    }

    fun getText(): String? {
        return text
    }

    fun setText(text: String?): DraftComment {
        this.text = text
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