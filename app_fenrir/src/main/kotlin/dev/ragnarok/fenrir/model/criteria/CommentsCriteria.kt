package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange
import dev.ragnarok.fenrir.model.Commented

class CommentsCriteria(val accountId: Long, val commented: Commented) {
    var range: DatabaseIdRange? = null
    fun setRange(range: DatabaseIdRange?): CommentsCriteria {
        this.range = range
        return this
    }
}