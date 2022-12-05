package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

/**
 * An university object describes an university.
 */
@Serializable
class VKApiUniversity : IUserActivityPoint {
    /**
     * University ID, positive number
     */
    var id = 0

    /**
     * ID of the country the university is located in, positive number
     */
    var country_id = 0

    /**
     * ID of the city the university is located in, positive number
     */
    var city_id = 0

    /**
     * University name
     */
    var name: String? = null

    /**
     * Faculty ID
     */
    var faculty = 0

    /**
     * Faculty name
     */
    var faculty_name: String? = null

    /**
     * University chair ID;
     */
    var chair = 0

    /**
     * Chair name
     */
    var chair_name: String? = null

    /**
     * Graduation year
     */
    var graduation = 0

    /**
     * Form of education
     */
    var education_form: String? = null

    /**
     * Status of education
     */
    var education_status: String? = null
}