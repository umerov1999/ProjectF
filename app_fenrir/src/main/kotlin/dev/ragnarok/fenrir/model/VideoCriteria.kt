package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.DatabaseIdRange
import dev.ragnarok.fenrir.model.criteria.Criteria

class VideoCriteria(
    private val accountId: Int,
    private val ownerId: Int,
    private val albumId: Int
) : Criteria() {
    private var range: DatabaseIdRange? = null
    fun getAccountId(): Int {
        return accountId
    }

    fun getAlbumId(): Int {
        return albumId
    }

    fun getOwnerId(): Int {
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