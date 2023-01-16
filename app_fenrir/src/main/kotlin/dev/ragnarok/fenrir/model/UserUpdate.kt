package dev.ragnarok.fenrir.model


class UserUpdate(val accountId: Long, val userId: Long) {

    var status: Status? = null

    var online: Online? = null

    class Online(val isOnline: Boolean, val lastSeen: Long, val platform: Int)

    class Status(val status: String)
}