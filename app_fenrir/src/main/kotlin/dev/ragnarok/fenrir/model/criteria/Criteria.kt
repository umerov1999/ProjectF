package dev.ragnarok.fenrir.model.criteria

open class Criteria : Cloneable {
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Criteria {
        return super.clone() as Criteria
    }
}