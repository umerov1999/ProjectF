package dev.ragnarok.fenrir.api.model.longpoll

class OutputMessagesSetReadUpdate :
    AbsLongpollEvent(ACTION_SET_OUTPUT_MESSAGES_AS_READ) {
    var peerId = 0L
    var localId = 0
    var unreadCount = 0
}