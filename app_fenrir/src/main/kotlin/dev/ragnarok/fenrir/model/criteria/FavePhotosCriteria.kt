package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class FavePhotosCriteria(val accountId: Int) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): FavePhotosCriteria {
        this.range = range
        return this
    }
}