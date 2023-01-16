package dev.ragnarok.fenrir.db.model

import dev.ragnarok.fenrir.db.model.entity.DboEntity
import dev.ragnarok.fenrir.db.model.entity.KeyboardEntity
import dev.ragnarok.fenrir.db.model.entity.MessageDboEntity

class MessageEditEntity(val status: Int, val senderId: Long) {
    var isEncrypted = false
        private set
    var date: Long = 0
        private set
    var isOut = false
        private set
    var isDeleted = false
        private set
    var isImportant = false
        private set
    var forward: List<MessageDboEntity>? = null
        private set
    var attachments: List<DboEntity>? = null
        private set
    var isRead = false
        private set
    var payload: String? = null
        private set
    var keyboard: KeyboardEntity? = null
        private set
    var body: String? = null
        private set
    var extras: Map<Int, String>? = null
        private set

    fun setBody(body: String?): MessageEditEntity {
        this.body = body
        return this
    }

    fun setKeyboard(keyboard: KeyboardEntity?): MessageEditEntity {
        this.keyboard = keyboard
        return this
    }

    fun setExtras(extras: Map<Int, String>?): MessageEditEntity {
        this.extras = extras
        return this
    }

    fun setRead(read: Boolean): MessageEditEntity {
        isRead = read
        return this
    }

    fun setEncrypted(encrypted: Boolean): MessageEditEntity {
        isEncrypted = encrypted
        return this
    }

    fun setDate(date: Long): MessageEditEntity {
        this.date = date
        return this
    }

    fun setOut(out: Boolean): MessageEditEntity {
        isOut = out
        return this
    }

    fun setDeleted(deleted: Boolean): MessageEditEntity {
        isDeleted = deleted
        return this
    }

    fun setImportant(important: Boolean): MessageEditEntity {
        isImportant = important
        return this
    }

    fun setForward(forward: List<MessageDboEntity>?): MessageEditEntity {
        this.forward = forward
        return this
    }

    fun setAttachments(attachments: List<DboEntity>?): MessageEditEntity {
        this.attachments = attachments
        return this
    }

    fun setPayload(payload: String?): MessageEditEntity {
        this.payload = payload
        return this
    }
}