package dev.ragnarok.fenrir.db.model

class MessagePatch(val messageId: Int, val peerId: Long) {
    var deletion: Deletion? = null
    var important: Important? = null

    class Deletion(val deleted: Boolean, val deletedForAll: Boolean)

    class Important(val important: Boolean)
}