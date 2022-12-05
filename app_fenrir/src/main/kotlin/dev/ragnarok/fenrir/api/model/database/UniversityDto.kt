package dev.ragnarok.fenrir.api.model.database

import kotlinx.serialization.Serializable

/**
 * A city object describes a University.
 */
@Serializable
class UniversityDto {
    /**
     * University ID.
     */
    var id = 0

    /**
     * University name
     */
    var title: String? = null
}