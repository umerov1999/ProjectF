package dev.ragnarok.fenrir.api.model.longpoll

class UserIsOfflineUpdate : AbsLongpollEvent(ACTION_USER_IS_OFFLINE) {
    var userId = 0
    var timestamp = 0
    var isTimeout = false
    var app_id = 0
}