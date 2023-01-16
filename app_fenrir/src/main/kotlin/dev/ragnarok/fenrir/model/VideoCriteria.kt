package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.DatabaseIdRange
import dev.ragnarok.fenrir.model.criteria.Criteria

class VideoCriteria(
    private val accountId: Long,
    private val ownerId: Long,
    private val albumId: Int
) : Criteria() {
    private var range: DatabaseIdRange? = null
    fun getAccountId(): Long {
        return accountId
    }

    fun getAlbumId(): Int {
        return albumId
    }

    fun getOwnerId(): Long {
        return ownerId
    }

    fun getRange(): DatabaseIdRange? {
        return range
    }

    fun setRange(range: DatabaseIdRange?): VideoCriteria {
        this.range = range
        return this
    }
}