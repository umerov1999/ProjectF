package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.DatabaseIdRange
import dev.ragnarok.fenrir.model.criteria.Criteria

class VideoAlbumCriteria(private val accountId: Int, private val ownerId: Int) : Criteria() {
    private var range: DatabaseIdRange? = null
    fun getOwnerId(): Int {
        return ownerId
    }

    fun getAccountId(): Int {
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