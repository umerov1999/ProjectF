package dev.ragnarok.fenrir.api.model

/**
 * Describes objects that contains an "id" field.
 */
interface Identificable {
    /**
     * Returns unique identifier of this object(usually it's value of JSON field "id").
     */
    fun getObjectId(): Int
}