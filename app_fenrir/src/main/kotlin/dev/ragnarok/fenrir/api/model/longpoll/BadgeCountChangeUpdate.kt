package dev.ragnarok.fenrir.api.model.longpoll

class BadgeCountChangeUpdate :
    AbsLongpollEvent(ACTION_COUNTER_UNREAD_WAS_CHANGED) {
    var count = 0
}