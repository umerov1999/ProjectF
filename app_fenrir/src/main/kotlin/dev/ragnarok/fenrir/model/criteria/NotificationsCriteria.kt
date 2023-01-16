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
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as NotificationsCriteria
        return (accountId == that.accountId
                && range == that.range)
    }

    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + if (range != null) range.hashCode() else 0
        return result
    }
}