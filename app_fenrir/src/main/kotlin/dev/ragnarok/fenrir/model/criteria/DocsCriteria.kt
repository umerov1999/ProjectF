package dev.ragnarok.fenrir.model.criteria

class DocsCriteria(val accountId: Long, val ownerId: Long) : Criteria() {
    var filter: Int? = null
        private set

    fun setFilter(filter: Int?): DocsCriteria {
        this.filter = filter
        return this
    }
}