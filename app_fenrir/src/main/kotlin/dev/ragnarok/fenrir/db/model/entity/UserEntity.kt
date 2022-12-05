package dev.ragnarok.fenrir.db.model.entity

import dev.ragnarok.fenrir.model.Sex
import dev.ragnarok.fenrir.model.UserPlatform

class UserEntity(val id: Int) {
    var firstName: String? = null
        private set
    var lastName: String? = null
        private set
    var isOnline = false
        private set
    var isOnlineMobile = false
        private set
    var onlineApp = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    var photoMax: String? = null
        private set
    var lastSeen: Long = 0
        private set
    var bdate: String? = null
        private set

    @get:UserPlatform
    @UserPlatform
    var platform = 0
        private set
    var status: String? = null
        private set

    @get:Sex
    @Sex
    var sex = 0
        private set
    var domain: String? = null
        private set
    var maiden_name: String? = null
        private set
    var isFriend = false
        private set
    var friendStatus = 0
        private set
    var canWritePrivateMessage = false
        private set
    var blacklisted_by_me = false
        private set
    var blacklisted = false
        private set
    var isVerified = false
        private set
    var isCan_access_closed = false
        private set

    fun setBdate(bdate: String?): UserEntity {
        this.bdate = bdate
        return this
    }

    fun setFirstName(firstName: String?): UserEntity {
        this.firstName = firstName
        return this
    }

    fun setLastName(lastName: String?): UserEntity {
        this.lastName = lastName
        return this
    }

    fun setOnline(online: Boolean): UserEntity {
        isOnline = online
        return this
    }

    fun setOnlineMobile(onlineMobile: Boolean): UserEntity {
        isOnlineMobile = onlineMobile
        return this
    }

    fun setOnlineApp(onlineApp: Int): UserEntity {
        this.onlineApp = onlineApp
        return this
    }

    fun setPhoto50(photo50: String?): UserEntity {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): UserEntity {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): UserEntity {
        this.photo200 = photo200
        return this
    }

    fun setLastSeen(lastSeen: Long): UserEntity {
        this.lastSeen = lastSeen
        return this
    }

    fun setPlatform(@UserPlatform platform: Int): UserEntity {
        this.platform = platform
        return this
    }

    fun setStatus(status: String?): UserEntity {
        this.status = status
        return this
    }

    fun setSex(@Sex sex: Int): UserEntity {
        this.sex = sex
        return this
    }

    fun setDomain(domain: String?): UserEntity {
        this.domain = domain
        return this
    }

    fun setFriend(friend: Boolean): UserEntity {
        isFriend = friend
        return this
    }

    fun setPhotoMax(photoMax: String?): UserEntity {
        this.photoMax = photoMax
        return this
    }

    fun setCanWritePrivateMessage(can_write_private_message: Boolean): UserEntity {
        canWritePrivateMessage = can_write_private_message
        return this
    }

    fun setBlacklisted_by_me(blacklisted_by_me: Boolean): UserEntity {
        this.blacklisted_by_me = blacklisted_by_me
        return this
    }

    fun setBlacklisted(blacklisted: Boolean): UserEntity {
        this.blacklisted = blacklisted
        return this
    }

    fun setFriendStatus(friendStatus: Int): UserEntity {
        this.friendStatus = friendStatus
        return this
    }

    fun setVerified(verified: Boolean): UserEntity {
        isVerified = verified
        return this
    }

    fun setCan_access_closed(can_access_closed: Boolean): UserEntity {
        isCan_access_closed = can_access_closed
        return this
    }

    fun setMaiden_name(maiden_name: String?): UserEntity {
        this.maiden_name = maiden_name
        return this
    }
}