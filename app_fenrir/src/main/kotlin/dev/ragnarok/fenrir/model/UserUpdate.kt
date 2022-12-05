package dev.ragnarok.fenrir.model


class UserUpdate(val accountId: Int, val userId: Int) {

    var status: Status? = null

    var online: Online? = null

    class Online(val isOnline: Boolean, val lastSeen: Long, val platform: Int)

    class Status(val status: String)
}