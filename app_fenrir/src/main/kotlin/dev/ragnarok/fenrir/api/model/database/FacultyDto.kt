package dev.ragnarok.fenrir.api.model.database

import kotlinx.serialization.Serializable

/**
 * A city object describes a Faculty.
 */
@Serializable
class FacultyDto {
    /**
     * Faculty ID.
     */
    var id = 0

    /**
     * Faculty name
     */
    var title: String? = null
}