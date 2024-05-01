package dev.ragnarok.fenrir.model

class GroupChats(val id: Int) {
    var members_count = 0
        private set
    var is_closed = false
        private set
    var invite_link: String? = null
        private set
    var photo: String? = null
        private set
    var title: String? = null
        private set
    var lastUpdateTime: Long = 0
        private set

    fun setMembers_count(members_count: Int): GroupChats {
        this.members_count = members_count
        return this
    }

    fun setLastUpdateTime(lastUpdateTime: Long): GroupChats {
        this.lastUpdateTime = lastUpdateTime
        return this
    }

    fun setIs_closed(is_closed: Boolean): GroupChats {
        this.is_closed = is_closed
        return this
    }

    fun setInvite_link(invite_link: String?): GroupChats {
        this.invite_link = invite_link
        return this
    }

    fun setPhoto(photo: String?): GroupChats {
        this.photo = photo
        return this
    }

    fun setTitle(title: String?): GroupChats {
        this.title = title
        return this
    }
}