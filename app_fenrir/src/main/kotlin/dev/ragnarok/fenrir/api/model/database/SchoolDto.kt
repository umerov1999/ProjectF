package dev.ragnarok.fenrir.api.model.database

import kotlinx.serialization.Serializable

/**
 * A city object describes a School.
 */
@Serializable
class SchoolDto {
    /**
     * School ID.
     */
    var id = 0

    /**
     * School name
     */
    var title: String? = null
}