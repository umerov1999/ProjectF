package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.DatabaseIdRange
import dev.ragnarok.fenrir.model.criteria.Criteria

class VideoCriteria(
    val accountId: Long,
    val ownerId: Long,
    val albumId: Int
) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): VideoCriteria {
        this.range = range
        return this
    }
}