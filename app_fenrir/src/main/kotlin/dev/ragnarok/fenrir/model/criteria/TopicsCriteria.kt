package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class TopicsCriteria(val accountId: Long, val ownerId: Long) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): TopicsCriteria {
        this.range = range
        return this
    }
}