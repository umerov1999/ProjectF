package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

/**
 * A school object describes a school.
 */
@Serializable
class VKApiMilitary
/**
 * Creates empty School instance.
 */
{
    /**
     * номер части
     */
    var unit: String? = null

    /**
     * идентификатор части в базе данных
     */
    var unit_id = 0

    /**
     * идентификатор страны, в которой находится часть
     */
    var country_id = 0

    /**
     * год начала службы
     */
    var from = 0

    /**
     * год окончания службы
     */
    var until = 0
}