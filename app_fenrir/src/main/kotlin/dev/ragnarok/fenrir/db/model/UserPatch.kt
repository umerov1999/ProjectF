package dev.ragnarok.fenrir.db.model

class UserPatch(val userId: Long) {
    var status: Status? = null
        private set
    var online: Online? = null
        private set

    fun setOnlineUpdate(online: Online?): UserPatch {
        this.online = online
        return this
    }

    fun setStatus(status: Status?): UserPatch {
        this.status = status
        return this
    }

    class Online(val isOnline: Boolean, val lastSeen: Long, val platform: Int)
    class Status(val status: String?)
}