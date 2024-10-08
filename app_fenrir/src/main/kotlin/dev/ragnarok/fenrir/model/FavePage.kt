package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.api.model.interfaces.IdentificableOwner

class FavePage(private val id: Long) : IdentificableOwner {
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

    override fun getOwnerObjectId(): Long {
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

    override fun equals(other: Any?): Boolean {
        return other is FavePage && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    val owner: Owner?
        get() {
            if (type == null) {
                return null
            }
            return when (type) {
                FavePageType.USER -> user
                FavePageType.COMMUNITY -> group
                else -> null
            }
        }
}