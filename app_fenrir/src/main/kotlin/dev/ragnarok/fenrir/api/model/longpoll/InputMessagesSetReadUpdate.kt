package dev.ragnarok.fenrir.api.model.longpoll

class InputMessagesSetReadUpdate :
    AbsLongpollEvent(ACTION_SET_INPUT_MESSAGES_AS_READ) {
    var peerId = 0
    var localId = 0
    var unreadCount = 0
}