package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class FaveArticlesCriteria(val accountId: Int) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): FaveArticlesCriteria {
        this.range = range
        return this
    }
}