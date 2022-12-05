package dev.ragnarok.fenrir.db

class PeerStateEntity(val peerId: Int) {
    var unreadCount = 0
        private set
    var lastMessageId = 0
        private set
    var inRead = 0
        private set
    var outRead = 0
        private set

    fun setUnreadCount(unreadCount: Int): PeerStateEntity {
        this.unreadCount = unreadCount
        return this
    }

    fun setLastMessageId(lastMessageId: Int): PeerStateEntity {
        this.lastMessageId = lastMessageId
        return this
    }

    fun setInRead(inRead: Int): PeerStateEntity {
        this.inRead = inRead
        return this
    }

    fun setOutRead(outRead: Int): PeerStateEntity {
        this.outRead = outRead
        return this
    }
}