package dev.ragnarok.fenrir.model

interface IOwnersBundle {
    fun findById(id: Long): Owner?
    fun getById(id: Long): Owner
    fun size(): Int
    fun putAll(owners: Collection<Owner>)
    fun put(owner: Owner)
    fun getMissing(ids: Collection<Long>?): Collection<Long>
}