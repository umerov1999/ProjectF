package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.model.entity.ReactionEntity

class MessageUpdate(private val accountId: Long, private val messageId: Int) {
    private var statusUpdate: StatusUpdate? = null
    private var importantUpdate: ImportantUpdate? = null
    private var deleteUpdate: DeleteUpdate? = null
    private var reactionUpdate: ReactionUpdate? = null
    fun getDeleteUpdate(): DeleteUpdate? {
        return deleteUpdate
    }

    fun setReactionUpdate(reactionUpdate: ReactionUpdate?) {
        this.reactionUpdate = reactionUpdate
    }

    fun getReactionUpdate(): ReactionUpdate? {
        return reactionUpdate
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

    class ReactionUpdate(
        private val peerId: Long,
        private val keepMyReaction: Boolean,
        private val reactionId: Int,
        private val reactions: List<ReactionEntity>
    ) {
        fun isKeepMyReaction(): Boolean {
            return keepMyReaction
        }

        fun reactionId(): Int {
            return reactionId
        }

        fun reactions(): List<ReactionEntity> {
            return reactions
        }

        fun peerId(): Long {
            return peerId
        }
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