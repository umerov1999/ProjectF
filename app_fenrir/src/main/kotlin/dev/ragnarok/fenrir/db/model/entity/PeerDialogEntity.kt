package dev.ragnarok.fenrir.db.model.entity

class PeerDialogEntity(val peerId: Long) {
    var title: String? = null
        private set
    var unreadCount = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    var inRead = 0
        private set
    var outRead = 0
        private set
    var pinned: MessageDboEntity? = null
        private set
    var currentKeyboard: KeyboardEntity? = null
        private set
    var lastMessageId = 0
        private set
    var isGroupChannel = false
        private set
    var acl = 0
        private set
    var major_id = 0
        private set
    var minor_id = 0
        private set

    fun setAcl(acl: Int): PeerDialogEntity {
        this.acl = acl
        return this
    }

    fun setLastMessageId(lastMessageId: Int): PeerDialogEntity {
        this.lastMessageId = lastMessageId
        return this
    }

    fun setPinned(pinned: MessageDboEntity?): PeerDialogEntity {
        this.pinned = pinned
        return this
    }

    fun setTitle(title: String?): PeerDialogEntity {
        this.title = title
        return this
    }

    fun setCurrentKeyboard(currentKeyboard: KeyboardEntity?): PeerDialogEntity {
        this.currentKeyboard = currentKeyboard
        return this
    }

    fun setUnreadCount(unreadCount: Int): PeerDialogEntity {
        this.unreadCount = unreadCount
        return this
    }

    fun setPhoto50(photo50: String?): PeerDialogEntity {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): PeerDialogEntity {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): PeerDialogEntity {
        this.photo200 = photo200
        return this
    }

    fun setInRead(inRead: Int): PeerDialogEntity {
        this.inRead = inRead
        return this
    }

    fun setOutRead(outRead: Int): PeerDialogEntity {
        this.outRead = outRead
        return this
    }

    fun setGroupChannel(groupChannel: Boolean): PeerDialogEntity {
        isGroupChannel = groupChannel
        return this
    }

    fun setMajor_id(major_id: Int): PeerDialogEntity {
        this.major_id = major_id
        return this
    }

    fun setMinor_id(minor_id: Int): PeerDialogEntity {
        this.minor_id = minor_id
        return this
    }
}