package dev.ragnarok.fenrir.db.model.entity

import dev.ragnarok.fenrir.model.FavePageType

class FavePageEntity(val id: Long) {
    var description: String? = null
        private set

    @FavePageType
    var faveType: String? = null
        private set
    var updateDate: Long = 0
        private set
    var user: UserEntity? = null
        private set
    var group: CommunityEntity? = null
        private set

    fun setDescription(description: String?): FavePageEntity {
        this.description = description
        return this
    }

    fun setFaveType(type: String?): FavePageEntity {
        faveType = type
        return this
    }

    fun setUpdateDate(updateDate: Long): FavePageEntity {
        this.updateDate = updateDate
        return this
    }

    fun setUser(user: UserEntity?): FavePageEntity {
        this.user = user
        return this
    }

    fun setGroup(group: CommunityEntity?): FavePageEntity {
        this.group = group
        return this
    }
}