package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class FaveProductsCriteria(val accountId: Int) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): FaveProductsCriteria {
        this.range = range
        return this
    }
}