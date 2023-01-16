package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class FaveVideosCriteria(val accountId: Long) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): FaveVideosCriteria {
        this.range = range
        return this
    }
}