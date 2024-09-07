package dev.ragnarok.fenrir.util.serializeble.json

import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Configuration of the current [Json] instance available through [Json.configuration]
 * and configured with [JsonBuilder] constructor.
 *
 * Can be used for debug purposes and for custom Json-specific serializers
 * via [JsonEncoder] and [JsonDecoder].
 *
 * Standalone configuration object is meaningless and can nor be used outside the
 * [Json], neither new [Json] instance can be created from it.
 *
 * Detailed description of each property is available in [JsonBuilder] class.
 */
class JsonConfiguration @OptIn(ExperimentalSerializationApi::class) internal constructor(
    val encodeDefaults: Boolean = false,
    val ignoreUnknownKeys: Boolean = false,
    val isLenient: Boolean = false,
    val allowStructuredMapKeys: Boolean = false,
    val prettyPrint: Boolean = false,
    val explicitNulls: Boolean = true,
    @ExperimentalSerializationApi val prettyPrintIndent: String = "    ",
    val coerceInputValues: Boolean = false,
    val useArrayPolymorphism: Boolean = false,
    val classDiscriminator: String = "type",
    val allowSpecialFloatingPointValues: Boolean = false,
    val useAlternativeNames: Boolean = true,
    @ExperimentalSerializationApi val namingStrategy: JsonNamingStrategy? = null,
    @ExperimentalSerializationApi val decodeEnumsCaseInsensitive: Boolean = false,
    @ExperimentalSerializationApi val allowTrailingComma: Boolean = false,
    @ExperimentalSerializationApi val allowComments: Boolean = false,
    @ExperimentalSerializationApi val classDiscriminatorMode: ClassDiscriminatorMode = ClassDiscriminatorMode.POLYMORPHIC,
) {

    /** @suppress Dokka **/
    @OptIn(ExperimentalSerializationApi::class)
    override fun toString(): String {
        return "JsonConfiguration(encodeDefaults=$encodeDefaults, ignoreUnknownKeys=$ignoreUnknownKeys, isLenient=$isLenient, " +
                "allowStructuredMapKeys=$allowStructuredMapKeys, prettyPrint=$prettyPrint, explicitNulls=$explicitNulls, " +
                "prettyPrintIndent='$prettyPrintIndent', coerceInputValues=$coerceInputValues, useArrayPolymorphism=$useArrayPolymorphism, " +
                "classDiscriminator='$classDiscriminator', allowSpecialFloatingPointValues=$allowSpecialFloatingPointValues, " +
                "useAlternativeNames=$useAlternativeNames, namingStrategy=$namingStrategy, decodeEnumsCaseInsensitive=$decodeEnumsCaseInsensitive, " +
                "allowTrailingComma=$allowTrailingComma, allowComments=$allowComments, classDiscriminatorMode=$classDiscriminatorMode)"
    }
}

/**
 * Defines which classes and objects should have their serial name included in the json as so-called class discriminator.
 *
 * Class discriminator is a JSON field added by kotlinx.serialization that has [JsonBuilder.classDiscriminator] as a key (`type` by default),
 * and class' serial name as a value (fully qualified name by default, can be changed with SerialName annotation).
 *
 * Class discriminator is important for serializing and deserializing [polymorphic class hierarchies](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#sealed-classes).
 * Default [ClassDiscriminatorMode.POLYMORPHIC] mode adds discriminator only to polymorphic classes.
 * This behavior can be changed to match various JSON schemas.
 *
 * @see JsonBuilder.classDiscriminator
 * @see JsonBuilder.classDiscriminatorMode
 * Polymorphic
 * PolymorphicSerializer
 */
enum class ClassDiscriminatorMode {
    /**
     * Never include class discriminator in the output.
     *
     * This mode is generally intended to produce JSON for consumption by third-party libraries.
     * kotlinx.serialization is unable to deserialize [polymorphic classes][POLYMORPHIC] without class discriminators,
     * so it is impossible to deserialize JSON produced in this mode if a data model has polymorphic classes.
     */
    NONE,

    /**
     * Include class discriminators whenever possible.
     *
     * Given that class discriminator is added as a JSON field, adding class discriminator is possible
     * when the resulting JSON is a json object — i.e., for Kotlin classes, `object`s, and interfaces.
     * More specifically, discriminator is added to the output of serializers which descriptors
     * have a SerialDescriptor.kind of either StructureKind.CLASS or StructureKind.OBJECT.
     *
     * This mode is generally intended to produce JSON for consumption by third-party libraries.
     * Given that [JsonBuilder.classDiscriminatorMode] does not affect deserialization, kotlinx.serialization
     * does not expect every object to have discriminator, which may trigger deserialization errors.
     * If you experience such problems, refrain from using [ALL_JSON_OBJECTS] or use [JsonBuilder.ignoreUnknownKeys].
     *
     * In the example:
     * ```
     * @Serializable class Plain(val p: String)
     * @Serializable sealed class Base
     * @Serializable object Impl: Base()
     *
     * @Serializable class All(val p: Plain, val b: Base, val i: Impl)
     * ```
     * setting [JsonBuilder.classDiscriminatorMode] to [ClassDiscriminatorMode.ALL_JSON_OBJECTS] adds
     * class discriminator to `All.p`, `All.b`, `All.i`, and to `All` object itself.
     */
    ALL_JSON_OBJECTS,

    /**
     * Include class discriminators for polymorphic classes.
     *
     * Sealed classes, abstract classes, and interfaces are polymorphic classes by definition.
     * Open classes can be polymorphic if they are serializable with PolymorphicSerializer
     * and properly registered in the SerializersModule.
     * See [kotlinx.serialization polymorphism guide](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#sealed-classes) for details.
     *
     * Note that implementations of polymorphic classes (e.g., sealed class inheritors) are not polymorphic classes from kotlinx.serialization standpoint.
     * This means that this mode adds class discriminators only if a statically known type of the property is a base class or interface.
     *
     * In the example:
     * ```
     * @Serializable class Plain(val p: String)
     * @Serializable sealed class Base
     * @Serializable object Impl: Base()
     *
     * @Serializable class All(val p: Plain, val b: Base, val i: Impl)
     * ```
     * setting [JsonBuilder.classDiscriminatorMode] to [ClassDiscriminatorMode.POLYMORPHIC] adds
     * class discriminator to `All.b`, but leaves `All.p` and `All.i` intact.
     *
     * SerializersModule
     * SerializersModuleBuilder
     * PolymorphicModuleBuilder
     */
    POLYMORPHIC,
}
