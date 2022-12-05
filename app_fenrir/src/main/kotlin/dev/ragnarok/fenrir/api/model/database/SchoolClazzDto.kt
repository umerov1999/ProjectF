package dev.ragnarok.fenrir.api.model.database

import dev.ragnarok.fenrir.api.adapters.SchoolClazzDtoAdapter
import kotlinx.serialization.Serializable

/**
 * A city object describes a SchoolClazz.
 */
@Serializable(with = SchoolClazzDtoAdapter::class)
class SchoolClazzDto {
    /**
     * SchoolClazz ID.
     */
    var id = 0

    /**
     * SchoolClazz name
     */
    var title: String? = null
}