package dev.ragnarok.fenrir.api.model.longpoll

class UserIsOnlineUpdate : AbsLongpollEvent(ACTION_USER_IS_ONLINE) {
    var userId = 0
    var platform = 0
    var timestamp = 0
    var app_id = 0
}