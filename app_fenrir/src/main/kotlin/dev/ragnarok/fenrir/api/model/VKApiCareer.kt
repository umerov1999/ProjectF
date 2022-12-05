package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

/**
 * A school object describes a school.
 */
@Serializable
class VKApiCareer
/**
 * Creates empty School instance.
 */
    : IUserActivityPoint {
    var group_id = 0
    var company: String? = null
    var country_id = 0
    var city_id = 0
    var from = 0
    var until = 0
    var position: String? = null
}