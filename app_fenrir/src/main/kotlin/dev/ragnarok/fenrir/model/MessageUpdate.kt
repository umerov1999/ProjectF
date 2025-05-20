package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.model.entity.ReactionEntity

class MessageUpdate(val accountId: Long, val messageId: Int) {
    var statusUpdate: StatusUpdate? = null
        private set
    var importantUpdate: ImportantUpdate? = null
        private set
    var deleteUpdate: DeleteUpdate? = null
        private set
    var reactionUpdate: ReactionUpdate? = null
        private set

    fun setReactionUpdate(reactionUpdate: ReactionUpdate?) {
        this.reactionUpdate = reactionUpdate
    }

    fun setDeleteUpdate(deleteUpdate: DeleteUpdate?) {
        this.deleteUpdate = deleteUpdate
    }

    fun setImportantUpdate(importantUpdate: ImportantUpdate?) {
        this.importantUpdate = importantUpdate
    }

    fun setStatusUpdate(statusUpdate: StatusUpdate?) {
        this.statusUpdate = statusUpdate
    }

    class ReactionUpdate(
        val peerId: Long,
        val keepMyReaction: Boolean,
        val reactionId: Int,
        val reactions: List<ReactionEntity>
    )

    class ImportantUpdate(val important: Boolean)
    class DeleteUpdate(val deleted: Boolean, val deletedForAll: Boolean)
    class StatusUpdate(@param:MessageStatus val status: Int, val vkid: Int?)
}