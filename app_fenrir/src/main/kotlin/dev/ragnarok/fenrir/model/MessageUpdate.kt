package dev.ragnarok.fenrir.model

class MessageUpdate(private val accountId: Long, private val messageId: Int) {
    private var statusUpdate: StatusUpdate? = null
    private var importantUpdate: ImportantUpdate? = null
    private var deleteUpdate: DeleteUpdate? = null
    fun getDeleteUpdate(): DeleteUpdate? {
        return deleteUpdate
    }

    fun setDeleteUpdate(deleteUpdate: DeleteUpdate?) {
        this.deleteUpdate = deleteUpdate
    }

    fun getImportantUpdate(): ImportantUpdate? {
        return importantUpdate
    }

    fun setImportantUpdate(importantUpdate: ImportantUpdate?) {
        this.importantUpdate = importantUpdate
    }

    fun getAccountId(): Long {
        return accountId
    }

    fun getMessageId(): Int {
        return messageId
    }

    fun getStatusUpdate(): StatusUpdate? {
        return statusUpdate
    }

    fun setStatusUpdate(statusUpdate: StatusUpdate?) {
        this.statusUpdate = statusUpdate
    }

    class ImportantUpdate(private val important: Boolean) {
        fun isImportant(): Boolean {
            return important
        }
    }

    class DeleteUpdate(private val deleted: Boolean, private val deletedForAll: Boolean) {
        fun isDeletedForAll(): Boolean {
            return deletedForAll
        }

        fun isDeleted(): Boolean {
            return deleted
        }
    }

    class StatusUpdate(@MessageStatus private val status: Int, private val vkid: Int?) {
        fun getVkid(): Int? {
            return vkid
        }

        @MessageStatus
        fun getStatus(): Int {
            return status
        }
    }
}