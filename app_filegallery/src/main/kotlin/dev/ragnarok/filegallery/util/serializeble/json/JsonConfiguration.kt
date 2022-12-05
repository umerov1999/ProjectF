package dev.ragnarok.filegallery.util.serializeble.json

import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Configuration of the current [Json] instance available through [Json.configuration]
 * and configured with [JsonBuilder] constructor.
 *
 * Can be used for debug purposes and for custom Json-specific serializers
 * via [JsonEncoder] and [JsonDecoder].
 *
 * Standalone configuration object is meaningless and can nor be used outside of the
 * [Json], neither new [Json] instance can be created from it.
 *
 * Detailed description of each property is available in [JsonBuilder] class.
 */
class JsonConfiguration internal constructor(
    val encodeDefaults: Boolean = false,
    val ignoreUnknownKeys: Boolean = false,
    val isLenient: Boolean = false,
    val allowStructuredMapKeys: Boolean = false,
    val prettyPrint: Boolean = false,
    @ExperimentalSerializationApi val explicitNulls: Boolean = true,
    @ExperimentalSerializationApi val prettyPrintIndent: String = "    ",
    val coerceInputValues: Boolean = false,
    val useArrayPolymorphism: Boolean = false,
    val classDiscriminator: String = "type",
    val allowSpecialFloatingPointValues: Boolean = false,
    val useAlternativeNames: Boolean = true
) {

    /** @suppress Dokka **/
    @OptIn(ExperimentalSerializationApi::class)
    override fun toString(): String {
        return "JsonConfiguration(encodeDefaults=$encodeDefaults, ignoreUnknownKeys=$ignoreUnknownKeys, isLenient=$isLenient, " +
                "allowStructuredMapKeys=$allowStructuredMapKeys, prettyPrint=$prettyPrint, explicitNulls=$explicitNulls, " +
                "prettyPrintIndent='$prettyPrintIndent', coerceInputValues=$coerceInputValues, useArrayPolymorphism=$useArrayPolymorphism, " +
                "classDiscriminator='$classDiscriminator', allowSpecialFloatingPointValues=$allowSpecialFloatingPointValues)"
    }
}
