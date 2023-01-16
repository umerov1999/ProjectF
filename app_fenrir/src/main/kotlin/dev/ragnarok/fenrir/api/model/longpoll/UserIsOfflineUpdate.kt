package dev.ragnarok.fenrir.api.model.longpoll

class UserIsOfflineUpdate : AbsLongpollEvent(ACTION_USER_IS_OFFLINE) {
    var userId = 0L
    var timestamp = 0L
    var isTimeout = false
    var app_id = 0
}