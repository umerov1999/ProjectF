package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.Serializable

/**
 * A country object describes a country.
 */
@Serializable
class VKApiCountry
/**
 * Creates empty Country instance.
 */
{
    /**
     * Country ID.
     */
    var id = 0

    /**
     * Country name
     */
    var title: String? = null
}