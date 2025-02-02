package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.db.DatabaseIdRange

class NotificationsCriteria(val accountId: Long) : Criteria() {
    var range: DatabaseIdRange? = null
        private set

    fun setRange(range: DatabaseIdRange?): NotificationsCriteria {
        this.range = range
        return this
    }

    override fun equals(other: Any?): Boolean {
        return other is NotificationsCriteria && accountId == other.accountId && range == other.range
    }

    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + (range?.hashCode() ?: 0)
        return result
    }
}