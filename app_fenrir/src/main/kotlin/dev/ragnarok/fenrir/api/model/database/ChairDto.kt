package dev.ragnarok.fenrir.api.model.database

import kotlinx.serialization.Serializable

/**
 * A city object describes a Chair.
 */
@Serializable
class ChairDto {
    /**
     * Chair ID.
     */
    var id = 0

    /**
     * Chair name
     */
    var title: String? = null
}