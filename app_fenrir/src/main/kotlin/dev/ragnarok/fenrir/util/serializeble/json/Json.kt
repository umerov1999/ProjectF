/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.ragnarok.fenrir.util.serializeble.json

import dev.ragnarok.fenrir.util.serializeble.json.DecodeSequenceMode.ARRAY_WRAPPED
import dev.ragnarok.fenrir.util.serializeble.json.DecodeSequenceMode.WHITESPACE_SEPARATED
import dev.ragnarok.fenrir.util.serializeble.json.internal.DescriptorSchemaCache
import dev.ragnarok.fenrir.util.serializeble.json.internal.FormatLanguage
import dev.ragnarok.fenrir.util.serializeble.json.internal.JsonDecodingException
import dev.ragnarok.fenrir.util.serializeble.json.internal.JsonToStringWriter
import dev.ragnarok.fenrir.util.serializeble.json.internal.PolymorphismValidator
import dev.ragnarok.fenrir.util.serializeble.json.internal.StreamingJsonDecoder
import dev.ragnarok.fenrir.util.serializeble.json.internal.WriteMode
import dev.ragnarok.fenrir.util.serializeble.json.internal.encodeByWriter
import dev.ragnarok.fenrir.util.serializeble.json.internal.lexer.StringJsonLexer
import dev.ragnarok.fenrir.util.serializeble.json.internal.readJson
import dev.ragnarok.fenrir.util.serializeble.json.internal.writeJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import okio.BufferedSource

/**
 * The main entry point to work with JSON serialization.
 * It is typically used by constructing an application-specific instance, with configured JSON-specific behaviour
 * and, if necessary, registered in [SerializersModule] custom serializers.
 * `Json` instance can be configured in its `Json {}` factory function using [JsonBuilder].
 * For demonstration purposes or trivial usages, Json [companion][Json.Default] can be used instead.
 *
 * Then constructed instance can be used either as regular [SerialFormat] or [StringFormat]
 * or for converting objects to [JsonElement] back and forth.
 *
 * This is the only serial format which has the first-class [JsonElement] support.
 * Any serializable class can be serialized to or from [JsonElement] with [Json.decodeFromJsonElement] and [Json.encodeToJsonElement] respectively or
 * serialize properties of [JsonElement] type.
 *
 * Example of usage:
 * ```
 * @Serializable
 * class DataHolder(val id: Int, val data: String, val extensions: JsonElement)
 *
 * val json = Json
 * val instance = DataHolder(42, "some data", buildJsonObject { put("additional key", "value") }
 *
 * // Plain StringFormat usage
 * val stringOutput: String = json.encodeToString(instance)
 *
 * // JsonElement serialization specific for JSON only
 * val jsonTree: JsonElement = json.encodeToJsonElement(instance)
 *
 * // Deserialize from string
 * val deserialized: DataHolder = json.decodeFromString<DataHolder>(stringOutput)
 *
 * // Deserialize from json tree, JSON-specific
 * val deserializedFromTree: DataHolder = json.decodeFromJsonElement<DataHolder>(jsonTree)
 *
 *  // Deserialize from string to JSON tree, JSON-specific
 *  val deserializedToTree: JsonElement = json.parseToJsonElement(stringOutput)
 * ```
 *
 * Json instance also exposes its [configuration] that can be used in custom serializers
 * that rely on [JsonDecoder] and [JsonEncoder] for customizable behaviour.
 */
sealed class Json(
    val configuration: JsonConfiguration,
    override val serializersModule: SerializersModule
) : StringFormat {

    @Deprecated(
        "Should not be accessed directly, use Json.schemaCache accessor instead",
        ReplaceWith("schemaCache"),
        DeprecationLevel.ERROR
    )
    internal val _schemaCache: DescriptorSchemaCache = DescriptorSchemaCache()

    /**
     * The default instance of [Json] with default configuration.
     */
    companion object Default : Json(JsonConfiguration(), EmptySerializersModule())

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    final override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val result = JsonToStringWriter()
        try {
            encodeByWriter(result, serializer, value)
            return result.toString()
        } finally {
            result.release()
        }
    }

    /**
     * Decodes and deserializes the given JSON [string] to the value of type [T] using deserializer
     * retrieved from the reified type parameter.
     *
     * @throws SerializationException in case of any decoding-specific error
     * @throws IllegalArgumentException if the decoded input is not a valid instance of [T]
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T> decodeFromString(
        @FormatLanguage(
            "json",
            "",
            ""
        ) string: String
    ): T =
        decodeFromString(serializersModule.serializer(), string)

    /**
     * Deserializes the given JSON [string] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON string is not a valid JSON input for the type [T]
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
     */
    @OptIn(InternalSerializationApi::class)
    final override fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        @FormatLanguage("json", "", "") string: String
    ): T {
        val lexer = StringJsonLexer(string)
        val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor, null)
        val result = input.decodeSerializableValue(deserializer)
        lexer.expectEof()
        return result
    }

    /**
     * Serializes the given [value] into an equivalent [JsonElement] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON
     */
    @OptIn(InternalSerializationApi::class)
    fun <T> encodeToJsonElement(serializer: SerializationStrategy<T>, value: T): JsonElement {
        return writeJson(value, serializer)
    }

    /**
     * Deserializes the given [element] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON element is not a valid JSON input for the type [T]
     * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
     */
    @OptIn(InternalSerializationApi::class)
    fun <T> decodeFromJsonElement(
        deserializer: DeserializationStrategy<T>,
        element: JsonElement
    ): T {
        return readJson(element, deserializer)
    }

    /**
     * Deserializes the given JSON [string] into a corresponding [JsonElement] representation.
     *
     * @throws [SerializationException] if the given string is not a valid JSON
     */
    fun parseToJsonElement(string: String): JsonElement {
        return decodeFromString(JsonElementSerializer, string)
    }

    fun parseToJsonElement(source: BufferedSource): JsonElement {
        return decodeFromBufferedSource(JsonElementSerializer, source)
    }

    fun printJsonElement(element: JsonElement): String {
        return encodeToString(JsonElementSerializer, element)
    }
}

/**
 * Description of JSON input shape used for decoding to sequence.
 *
 * The sequence represents a stream of objects parsed one by one;
 * [DecodeSequenceMode] defines a separator between these objects.
 * Typically, these objects are not separated by meaningful characters ([WHITESPACE_SEPARATED]),
 * or the whole stream is a large array of objects separated with commas ([ARRAY_WRAPPED]).
 */
@ExperimentalSerializationApi
enum class DecodeSequenceMode {
    /**
     * Declares that objects in the input stream are separated by whitespace characters.
     *
     * The stream is read as multiple JSON objects separated by any number of whitespace characters between objects. Starting and trailing whitespace characters are also permitted.
     * Each individual object is parsed lazily, when it is requested from the resulting sequence.
     *
     * Whitespace character is either ' ', '\n', '\r' or '\t'.
     *
     * Example of `WHITESPACE_SEPARATED` stream content:
     * ```
     * """{"key": "value"}{"key": "value2"}   {"key2": "value2"}"""
     * ```
     */
    WHITESPACE_SEPARATED,

    /**
     * Declares that objects in the input stream are wrapped in the JSON array.
     * Each individual object in the array is parsed lazily when it is requested from the resulting sequence.
     *
     * The stream is read as multiple JSON objects wrapped into a JSON array.
     * The stream must start with an array start character `[` and end with an array end character `]`,
     * otherwise, [JsonDecodingException] is thrown.
     *
     * Example of `ARRAY_WRAPPED` stream content:
     * ```
     * """[{"key": "value"}, {"key": "value2"},{"key2": "value2"}]"""
     * ```
     */
    ARRAY_WRAPPED,

    /**
     * Declares that parser itself should select between [WHITESPACE_SEPARATED] and [ARRAY_WRAPPED] modes.
     * The selection is performed by looking at the first meaningful character of the stream.
     *
     * In most cases, auto-detection is sufficient to correctly parse an input.
     * If the input is _whitespace-separated stream of the arrays_, parser could select an incorrect mode,
     * for that [DecodeSequenceMode] must be specified explicitly.
     *
     * Example of an exceptional case:
     * `[1, 2, 3]   [4, 5, 6]\n[7, 8, 9]`
     */
    AUTO_DETECT;
}

/**
 * Creates an instance of [Json] configured from the optionally given [Json instance][from] and adjusted with [builderAction].
 */
fun Json(from: Json = Json.Default, builderAction: JsonBuilder.() -> Unit): Json {
    val builder = JsonBuilder(from)
    builder.builderAction()
    val conf = builder.build()
    return JsonImpl(conf, builder.serializersModule)
}

/**
 * Serializes the given [value] into an equivalent [JsonElement] using a serializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
inline fun <reified T> Json.encodeToJsonElement(value: T): JsonElement {
    return encodeToJsonElement(serializersModule.serializer(), value)
}

/**
 * Deserializes the given [json] element into a value of type [T] using a deserializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given JSON element is not a valid JSON input for the type [T]
 * @throws [IllegalArgumentException] if the decoded input cannot be represented as a valid instance of type [T]
 */
inline fun <reified T> Json.decodeFromJsonElement(json: JsonElement): T =
    decodeFromJsonElement(serializersModule.serializer(), json)

inline fun <reified T> Json.decodeFromJsonElementOrNull(json: JsonElement?): T? =
    json?.let { decodeFromJsonElement(serializersModule.serializer(), it) }

inline fun <reified T> Json.decodeFromJsonElementOrNull(
    serializer: KSerializer<T>,
    json: JsonElement?
): T? =
    json?.let { decodeFromJsonElement(serializer, it) }

/**
 * Builder of the [Json] instance provided by `Json { ... }` factory function.
 */
@Suppress("unused", "DeprecatedCallableAddReplaceWith")
@OptIn(ExperimentalSerializationApi::class)
class JsonBuilder internal constructor(json: Json) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     */
    var encodeDefaults: Boolean = json.configuration.encodeDefaults

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     *
     * When this flag is disabled properties with `null` values without default are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     *
     * `true` by default.
     */
    @ExperimentalSerializationApi
    var explicitNulls: Boolean = json.configuration.explicitNulls

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    var ignoreUnknownKeys: Boolean = json.configuration.ignoreUnknownKeys

    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode quoted boolean literals,
     * and unquoted string literals are allowed.
     *
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid value in the input, replacing them with defaults.
     *
     * `false` by default.
     */
    var isLenient: Boolean = json.configuration.isLenient

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     * `false` by default.
     */
    var allowStructuredMapKeys: Boolean = json.configuration.allowStructuredMapKeys

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     *  `false` by default.
     */
    var prettyPrint: Boolean = json.configuration.prettyPrint

    /**
     * Specifies indent string to use with [prettyPrint] mode
     * 4 spaces by default.
     * Experimentality note: this API is experimental because
     * it is not clear whether this option has compelling use-cases.
     */
    @ExperimentalSerializationApi
    var prettyPrintIndent: String = json.configuration.prettyPrintIndent

    /**
     * Enables coercing incorrect JSON values to the default property value in the following cases:
     *   1. JSON value is `null` but the property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains unknown enum member.
     *
     * `false` by default.
     */
    var coerceInputValues: Boolean = json.configuration.coerceInputValues

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     * `false` by default.
     */
    var useArrayPolymorphism: Boolean = json.configuration.useArrayPolymorphism

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * "type" by default.
     */
    var classDiscriminator: String = json.configuration.classDiscriminator

    /**
     * Removes JSON specification restriction on
     * special floating-point values such as `NaN` and `Infinity` and enables their serialization and deserialization.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     * `false` by default.
     */
    var allowSpecialFloatingPointValues: Boolean =
        json.configuration.allowSpecialFloatingPointValues

    /**
     * Specifies whether Json instance makes use of [JsonNames] annotation.
     *
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large count of fields is skipped with [ignoreUnknownKeys].
     * `true` by default.
     */
    var useAlternativeNames: Boolean = json.configuration.useAlternativeNames

    /**
     * Specifies [JsonNamingStrategy] that should be used for all properties in classes for serialization and deserialization.
     *
     * `null` by default.
     *
     * This strategy is applied for all entities that have StructureKind.CLASS.
     */
    @ExperimentalSerializationApi
    var namingStrategy: JsonNamingStrategy? = json.configuration.namingStrategy

    /**
     * Enables decoding enum values in a case-insensitive manner.
     * Encoding is not affected.
     *
     * This affects both enum serial names and alternative names (specified with the [JsonNames] annotation).
     * In the following example, string `[VALUE_A, VALUE_B]` will be printed:
     * ```
     * enum class E { VALUE_A, @JsonNames("ALTERNATIVE") VALUE_B }
     *
     * @Serializable
     * data class Outer(val enums: List<E>)
     *
     * val j = Json { decodeEnumsCaseInsensitive = true }
     * println(j.decodeFromString<Outer>("""{"enums":["value_A", "alternative"]}""").enums)
     * ```
     *
     * If this feature is enabled,
     * it is no longer possible to decode enum values that have the same name in a lowercase form.
     * The following code will throw a serialization exception:
     *
     * ```
     * enum class BadEnum { Bad, BAD }
     * val j = Json { decodeEnumsCaseInsensitive = true }
     * j.decodeFromString<Box<BadEnum>>("""{"boxed":"bad"}""")
     * ```
     */
    @ExperimentalSerializationApi
    var decodeEnumsCaseInsensitive: Boolean = json.configuration.decodeEnumsCaseInsensitive

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Json] instance.
     *
     * @see SerializersModule
     * @see Contextual
     * @see Polymorphic
     */
    var serializersModule: SerializersModule = json.serializersModule

    @OptIn(ExperimentalSerializationApi::class)
    internal fun build(): JsonConfiguration {
        if (useArrayPolymorphism) require(classDiscriminator == defaultDiscriminator) {
            "Class discriminator should not be specified when array polymorphism is specified"
        }

        if (!prettyPrint) {
            require(prettyPrintIndent == defaultIndent) {
                "Indent should not be specified when default printing mode is used"
            }
        } else if (prettyPrintIndent != defaultIndent) {
            // Values allowed by JSON specification as whitespaces
            val allWhitespaces =
                prettyPrintIndent.all { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
            require(allWhitespaces) {
                "Only whitespace, tab, newline and carriage return are allowed as pretty print symbols. Had $prettyPrintIndent"
            }
        }

        return JsonConfiguration(
            encodeDefaults, ignoreUnknownKeys, isLenient,
            allowStructuredMapKeys, prettyPrint, explicitNulls, prettyPrintIndent,
            coerceInputValues, useArrayPolymorphism,
            classDiscriminator, allowSpecialFloatingPointValues, useAlternativeNames,
            namingStrategy, decodeEnumsCaseInsensitive
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class JsonImpl(configuration: JsonConfiguration, module: SerializersModule) :
    Json(configuration, module) {

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serializersModule == EmptySerializersModule()) return // Fast-path for in-place JSON allocations
        val collector = PolymorphismValidator(
            configuration.useArrayPolymorphism,
            configuration.classDiscriminator
        )
        serializersModule.dumpTo(collector)
    }
}

private const val defaultIndent = "    "
private const val defaultDiscriminator = "type"
