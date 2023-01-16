package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.DatabaseIdRange
import dev.ragnarok.fenrir.model.criteria.Criteria

class VideoAlbumCriteria(private val accountId: Long, private val ownerId: Long) : Criteria() {
    private var range: DatabaseIdRange? = null
    fun getOwnerId(): Long {
        return ownerId
    }

    fun getAccountId(): Long {
        return accountId
    }

    fun getRange(): DatabaseIdRange? {
        return range
    }

    fun setRange(range: DatabaseIdRange?): VideoAlbumCriteria {
        this.range = range
        return this
    }
}