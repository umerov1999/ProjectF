package dev.ragnarok.fenrir.api.model.interfaces

/**
 * Describes objects that contains an "id" field.
 */
interface IdentificableOwner {
    /**
     * Returns unique identifier of this object(usually it's value of JSON field "id").
     */
    fun getOwnerObjectId(): Long
}