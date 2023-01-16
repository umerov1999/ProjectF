package dev.ragnarok.fenrir.api.model.longpoll

class InputMessagesSetReadUpdate :
    AbsLongpollEvent(ACTION_SET_INPUT_MESSAGES_AS_READ) {
    var peerId = 0L
    var localId = 0
    var unreadCount = 0

    fun set(peerId: Long, localId: Int, unreadCount: Int): InputMessagesSetReadUpdate {
        this.peerId = peerId
        this.localId = localId
        this.unreadCount = unreadCount
        return this
    }
}