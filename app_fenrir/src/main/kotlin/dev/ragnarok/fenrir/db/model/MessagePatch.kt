package dev.ragnarok.fenrir.db.model

import dev.ragnarok.fenrir.db.model.entity.ReactionEntity

class MessagePatch(val messageId: Int, val peerId: Long) {
    var deletion: Deletion? = null
    var important: Important? = null
    var reaction: ReactionUpdate? = null

    class Deletion(val deleted: Boolean, val deletedForAll: Boolean)

    class Important(val important: Boolean)

    class ReactionUpdate(
        val keepMyReaction: Boolean,
        val reactionId: Int,
        val reactions: List<ReactionEntity>
    )
}