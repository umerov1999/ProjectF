/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonElementSerializer
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.internal.lexer.COLON
import kotlinx.serialization.json.internal.lexer.COMMA
import kotlinx.serialization.json.internal.lexer.INVALID
import kotlinx.serialization.json.internal.lexer.NULL
import kotlinx.serialization.json.jsonUnquotedLiteralDescriptor
import kotlinx.serialization.modules.SerializersModule

private val unsignedNumberDescriptors = setOf(
    UInt.serializer().descriptor,
    ULong.serializer().descriptor,
    UByte.serializer().descriptor,
    UShort.serializer().descriptor
)

internal val SerialDescriptor.isUnsignedNumber: Boolean
    get() = this.isInline && this in unsignedNumberDescriptors

internal val SerialDescriptor.isUnquotedLiteral: Boolean
    get() = this.isInline && this == jsonUnquotedLiteralDescriptor

@OptIn(ExperimentalSerializationApi::class)
internal class StreamingJsonEncoder(
    private val composer: Composer,
    override val json: Json,
    private val mode: WriteMode,
    private val modeReuseCache: Array<JsonEncoder?>?
) : JsonEncoder, AbstractEncoder() {

    internal constructor(
        output: InternalJsonWriter, json: Json, mode: WriteMode,
        modeReuseCache: Array<JsonEncoder?>
    ) : this(Composer(output, json), json, mode, modeReuseCache)

    override val serializersModule: SerializersModule = json.serializersModule
    private val configuration = json.configuration

    // Forces serializer to wrap all values into quotes
    private var forceQuoting: Boolean = false
    private var polymorphicDiscriminator: String? = null
    private var polymorphicSerialName: String? = null

    init {
        val i = mode.ordinal
        if (modeReuseCache != null) {
            if (modeReuseCache[i] !== null || modeReuseCache[i] !== this)
                modeReuseCache[i] = this
        }
    }

    override fun encodeJsonElement(element: JsonElement) {
        if (polymorphicDiscriminator != null && element !is JsonObject) {
            throwJsonElementPolymorphicException(polymorphicSerialName, element)
        }
        encodeSerializableValue(JsonElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
        return configuration.encodeDefaults
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        encodePolymorphically(serializer, value) { discriminatorName, serialName ->
            polymorphicDiscriminator = discriminatorName
            polymorphicSerialName = serialName
        }
    }

    private fun encodeTypeInfo(discriminator: String, serialName: String) {
        composer.nextItem()
        encodeString(discriminator)
        composer.print(COLON)
        composer.space()
        encodeString(serialName)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val newMode = json.switchMode(descriptor)
        if (newMode.begin != INVALID) { // entry
            composer.print(newMode.begin)
            composer.indent()
        }

        val discriminator = polymorphicDiscriminator
        if (discriminator != null) {
            encodeTypeInfo(discriminator, polymorphicSerialName ?: descriptor.serialName)
            polymorphicDiscriminator = null
            polymorphicSerialName = null
        }

        if (mode == newMode) {
            return this
        }

        return modeReuseCache?.get(newMode.ordinal) ?: StreamingJsonEncoder(
            composer,
            json,
            newMode,
            modeReuseCache
        )
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (mode.end != INVALID) {
            composer.unIndent()
            composer.nextItemIfNotFirst()
            composer.print(mode.end)
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        when (mode) {
            WriteMode.LIST -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
            }

            WriteMode.MAP -> {
                if (!composer.writingFirst) {
                    forceQuoting = if (index % 2 == 0) {
                        composer.print(COMMA)
                        composer.nextItem() // indent should only be put after commas in map
                        true
                    } else {
                        composer.print(COLON)
                        composer.space()
                        false
                    }
                } else {
                    forceQuoting = true
                    composer.nextItem()
                }
            }

            WriteMode.POLY_OBJ -> {
                if (index == 0)
                    forceQuoting = true
                if (index == 1) {
                    composer.print(COMMA)
                    composer.space()
                    forceQuoting = false
                }
            }

            else -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
                encodeString(descriptor.getJsonElementName(json, index))
                composer.print(COLON)
                composer.space()
            }
        }
        return true
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null || configuration.explicitNulls) {
            super.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        when {
            descriptor.isUnsignedNumber -> StreamingJsonEncoder(
                composerAs(::ComposerForUnsignedNumbers),
                json,
                mode,
                null
            )

            descriptor.isUnquotedLiteral -> StreamingJsonEncoder(
                composerAs(::ComposerForUnquotedLiterals),
                json,
                mode,
                null
            )

            polymorphicDiscriminator != null -> apply {
                polymorphicSerialName = descriptor.serialName
            }

            else -> super.encodeInline(descriptor)
        }

    private inline fun <reified T : Composer> composerAs(composerCreator: (writer: InternalJsonWriter, forceQuoting: Boolean) -> T): T {
        // If we're inside encodeInline().encodeSerializableValue, we should preserve the forceQuoting state
        // inside the composer, but not in the encoder (otherwise we'll get into `if (forceQuoting) encodeString(value.toString())` part
        // and unsigned numbers would be encoded incorrectly)
        return composer as? T ?: composerCreator(composer.writer, forceQuoting)
    }

    override fun encodeNull() {
        composer.print(NULL)
    }

    override fun encodeBoolean(value: Boolean) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeByte(value: Byte) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeShort(value: Short) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeInt(value: Int) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeLong(value: Long) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeFloat(value: Float) {
        // First encode value, then check, to have a prettier error message
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value, composer.writer.toString())
        }
    }

    override fun encodeDouble(value: Double) {
        // First encode value, then check, to have a prettier error message
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value, composer.writer.toString())
        }
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeString(value: String) = composer.printQuoted(value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }
}
