package dev.ragnarok.fenrir.db.model

import dev.ragnarok.fenrir.db.model.entity.MessageDboEntity

class PeerPatch(val id: Int) {
    var inRead: ReadTo? = null
        private set
    var outRead: ReadTo? = null
        private set
    var unread: Unread? = null
        private set
    var lastMessage: LastMessage? = null
        private set
    var pin: Pin? = null
        private set
    var title: Title? = null
        private set

    fun withInRead(id: Int): PeerPatch {
        inRead = ReadTo(id)
        return this
    }

    fun withOutRead(id: Int): PeerPatch {
        outRead = ReadTo(id)
        return this
    }

    fun withUnreadCount(count: Int): PeerPatch {
        unread = Unread(count)
        return this
    }

    fun withLastMessage(id: Int): PeerPatch {
        lastMessage = LastMessage(id)
        return this
    }

    fun withPin(pinned: MessageDboEntity?): PeerPatch {
        pin = Pin(pinned)
        return this
    }

    fun withTitle(title: String?): PeerPatch {
        this.title = Title(title)
        return this
    }

    class Title internal constructor(val title: String?)
    class Unread internal constructor(val count: Int)
    class Pin internal constructor(val pinned: MessageDboEntity?)
    class LastMessage internal constructor(val id: Int)
    class ReadTo(val id: Int)
}