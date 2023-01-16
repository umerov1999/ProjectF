package dev.ragnarok.fenrir.db.model.entity

class CommunityEntity(val id: Long) {
    var name: String? = null
        private set
    var screenName: String? = null
        private set
    var closed = 0
        private set
    var isAdmin = false
        private set
    var adminLevel = 0
        private set
    var isMember = false
        private set
    var membersCount = 0
        private set
    var memberStatus = 0
        private set
    var type = 0
        private set
    var photo50: String? = null
        private set
    var photo100: String? = null
        private set
    var photo200: String? = null
        private set
    var isVerified = false
        private set
    var isBlacklisted = false
        private set
    var hasUnseenStories = false
        private set

    fun setHasUnseenStories(hasUnseenStories: Boolean): CommunityEntity {
        this.hasUnseenStories = hasUnseenStories
        return this
    }

    fun setName(name: String?): CommunityEntity {
        this.name = name
        return this
    }

    fun setScreenName(screenName: String?): CommunityEntity {
        this.screenName = screenName
        return this
    }

    fun setClosed(closed: Int): CommunityEntity {
        this.closed = closed
        return this
    }

    fun setBlacklisted(isBlacklisted: Boolean): CommunityEntity {
        this.isBlacklisted = isBlacklisted
        return this
    }

    fun setAdmin(admin: Boolean): CommunityEntity {
        isAdmin = admin
        return this
    }

    fun setAdminLevel(adminLevel: Int): CommunityEntity {
        this.adminLevel = adminLevel
        return this
    }

    fun setMember(member: Boolean): CommunityEntity {
        isMember = member
        return this
    }

    fun setMembersCount(membersCount: Int): CommunityEntity {
        this.membersCount = membersCount
        return this
    }

    fun setMemberStatus(memberStatus: Int): CommunityEntity {
        this.memberStatus = memberStatus
        return this
    }

    fun setType(type: Int): CommunityEntity {
        this.type = type
        return this
    }

    fun setPhoto50(photo50: String?): CommunityEntity {
        this.photo50 = photo50
        return this
    }

    fun setPhoto100(photo100: String?): CommunityEntity {
        this.photo100 = photo100
        return this
    }

    fun setPhoto200(photo200: String?): CommunityEntity {
        this.photo200 = photo200
        return this
    }

    fun setVerified(verified: Boolean): CommunityEntity {
        isVerified = verified
        return this
    }
}