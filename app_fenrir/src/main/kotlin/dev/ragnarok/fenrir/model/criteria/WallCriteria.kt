package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class WallCriteria(val accountId: Long, val ownerId: Long) : Criteria() {
    var range: DatabaseIdRange? = null
        private set
    var mode: Int
        private set

    fun setMode(mode: Int): WallCriteria {
        this.mode = mode
        return this
    }

    fun setRange(range: DatabaseIdRange?): WallCriteria {
        this.range = range
        return this
    }

    override fun toString(): String {
        return "WallCriteria{" +
                "accountId=" + accountId +
                ", range=" + range +
                ", ownerId=" + ownerId +
                ", mode=" + mode +
                '}'
    }

    companion object {
        const val MODE_ALL = 0
        const val MODE_OWNER = 1
        const val MODE_SCHEDULED = 2
        const val MODE_SUGGEST = 3
        const val MODE_DONUT = 4
    }

    init {
        mode = MODE_ALL
    }
}