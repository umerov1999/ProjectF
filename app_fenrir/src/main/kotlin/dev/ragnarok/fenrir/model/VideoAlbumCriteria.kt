package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.db.DatabaseIdRange
import dev.ragnarok.fenrir.model.criteria.Criteria

class VideoAlbumCriteria(val accountId: Long, val ownerId: Long) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): VideoAlbumCriteria {
        this.range = range
        return this
    }
}