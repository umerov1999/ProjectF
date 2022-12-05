package dev.ragnarok.fenrir.api.model.longpoll

class MessageFlagsSetUpdate :
    AbsLongpollEvent(ACTION_MESSAGES_FLAGS_SET) {
    //[[2,1030891,128,216143660]]
    var messageId = 0
    var mask = 0
    var peerId = 0
}