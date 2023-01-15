package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.IUserActivityPoint
import kotlinx.serialization.Serializable

/**
 * A school object describes a school.
 */
@Serializable
class VKApiSchool : IUserActivityPoint {
    /**
     * School ID, positive number
     */
    var id = 0

    /**
     * ID of the country the school is located in, positive number
     */
    var country_id = 0

    /**
     * ID of the city the school is located in, positive number
     */
    var city_id = 0

    /**
     * School name
     */
    var name: String? = null

    /**
     * Year the user started to study
     */
    var year_from = 0

    /**
     * Year the user finished to study
     */
    var year_to = 0

    /**
     * Graduation year
     */
    var year_graduated = 0

    /**
     * School class letter
     */
    var clazz: String? = null

    /**
     * Speciality
     */
    var speciality: String? = null

    /**
     * идентификатор типа
     */
    var type = 0

    /**
     * название типа
     */
    var type_str: String? = null
    private val fullName: String? = null
}