package dev.ragnarok.fenrir.db.model.entity

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class FeedOwnersEntity {
    @Transient
    var id: Long = 0
        private set

    var title: String? = null
        private set
    var ownersIds: LongArray? = null
        private set

    fun setId(id: Long): FeedOwnersEntity {
        this.id = id
        return this
    }

    fun setTitle(title: String?): FeedOwnersEntity {
        this.title = title
        return this
    }

    fun setOwnersIds(ownersIds: LongArray?): FeedOwnersEntity {
        this.ownersIds = ownersIds
        return this
    }
}