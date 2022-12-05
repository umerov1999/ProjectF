package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.api.model.Identificable

class FavePage(private val id: Int) : Identificable {
    var description: String? = null
        private set

    @get:FavePageType
    var type: String? = null
        private set
    var updatedDate: Long = 0
        private set
    var user: User? = null
        private set
    var group: Community? = null
        private set

    override fun getObjectId(): Int {
        return id
    }

    fun setDescription(description: String?): FavePage {
        this.description = description
        return this
    }

    fun setFaveType(@FavePageType type: String?): FavePage {
        this.type = type
        return this
    }

    fun setUpdatedDate(updateDate: Long): FavePage {
        updatedDate = updateDate
        return this
    }

    fun setUser(user: User?): FavePage {
        this.user = user
        return this
    }

    fun setGroup(group: Community?): FavePage {
        this.group = group
        return this
    }

    val owner: Owner?
        get() {
            if (type == null) {
                return null
            }
            when (type) {
                FavePageType.USER -> return user
                FavePageType.COMMUNITY -> return group
            }
            return null
        }
}