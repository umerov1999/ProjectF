package dev.ragnarok.fenrir.api.model.longpoll

class InputMessagesSetReadUpdate :
    AbsLongpollEvent(ACTION_SET_INPUT_MESSAGES_AS_READ) {
    var peerId = 0
    var localId = 0
    var unreadCount = 0

    fun set(peerId: Int, localId: Int, unreadCount: Int): InputMessagesSetReadUpdate {
        this.peerId = peerId
        this.localId = localId
        this.unreadCount = unreadCount
        return this
    }
}