package dev.ragnarok.fenrir.model.criteria

open class Criteria : Cloneable {
    @Throws(CloneNotSupportedException::class)
    override fun clone(): Criteria {
        return super.clone() as Criteria
    }
}