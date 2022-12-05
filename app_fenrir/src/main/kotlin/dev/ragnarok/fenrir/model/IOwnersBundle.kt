package dev.ragnarok.fenrir.model

interface IOwnersBundle {
    fun findById(id: Int): Owner?
    fun getById(id: Int): Owner
    fun size(): Int
    fun putAll(owners: Collection<Owner>)
    fun put(owner: Owner)
    fun getMissing(ids: Collection<Int>?): Collection<Int>
}