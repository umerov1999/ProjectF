package dev.ragnarok.fenrir.model

class GroupChats(private val id: Int) {
    private var members_count = 0
    private var is_closed = false
    private var invite_link: String? = null
    private var photo: String? = null
    private var title: String? = null
    private var lastUpdateTime: Long = 0
    fun getId(): Int {
        return id
    }

    fun getMembers_count(): Int {
        return members_count
    }

    fun setMembers_count(members_count: Int): GroupChats {
        this.members_count = members_count
        return this
    }

    fun getLastUpdateTime(): Long {
        return lastUpdateTime
    }

    fun setLastUpdateTime(lastUpdateTime: Long): GroupChats {
        this.lastUpdateTime = lastUpdateTime
        return this
    }

    fun isIs_closed(): Boolean {
        return is_closed
    }

    fun setIs_closed(is_closed: Boolean): GroupChats {
        this.is_closed = is_closed
        return this
    }

    fun getInvite_link(): String? {
        return invite_link
    }

    fun setInvite_link(invite_link: String?): GroupChats {
        this.invite_link = invite_link
        return this
    }

    fun getPhoto(): String? {
        return photo
    }

    fun setPhoto(photo: String?): GroupChats {
        this.photo = photo
        return this
    }

    fun getTitle(): String? {
        return title
    }

    fun setTitle(title: String?): GroupChats {
        this.title = title
        return this
    }
}