// SPDX-FileCopyrightText: 2020-2021 Eduard Wolf
//
// SPDX-License-Identifier: Apache-2.0

package dev.ragnarok.filegallery.util.serializeble.prefs.encoding

import android.content.SharedPreferences
import dev.ragnarok.filegallery.util.serializeble.prefs.DoubleRepresentation
import dev.ragnarok.filegallery.util.serializeble.prefs.Preferences
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.internal.NamedValueEncoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
@Suppress("TooManyFunctions")
internal class PreferenceEncoder(
    private val preferences: Preferences,
    private val editor: SharedPreferences.Editor,
    private val sharedPreferences: SharedPreferences,
) : NamedValueEncoder() {

    override val serializersModule: SerializersModule = preferences.serializersModule

    internal fun pushInitialTag(name: String) {
        val tag = nested(name)
        pushTag(tag)
        editor.remove(tag)
        sharedPreferences.all.keys
            .filter { it.startsWith("$tag.") }
            .forEach { editor.remove(it) }
    }

    override fun encodeTaggedNull(tag: String) {
        editor.remove(tag)
    }

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
        editor.putString(tag, enumDescriptor.getElementName(ordinal))
    }

    override fun encodeTaggedBoolean(tag: String, value: Boolean) {
        editor.putBoolean(tag, value)
    }

    override fun encodeTaggedByte(tag: String, value: Byte) {
        editor.putInt(tag, value.toInt())
    }

    override fun encodeTaggedShort(tag: String, value: Short) {
        editor.putInt(tag, value.toInt())
    }

    override fun encodeTaggedInt(tag: String, value: Int) {
        editor.putInt(tag, value)
    }

    override fun encodeTaggedLong(tag: String, value: Long) {
        editor.putLong(tag, value)
    }

    override fun encodeTaggedFloat(tag: String, value: Float) {
        editor.putFloat(tag, value)
    }

    override fun encodeTaggedDouble(tag: String, value: Double) {
        when (preferences.configuration.doubleRepresentation) {
            DoubleRepresentation.FLOAT -> encodeTaggedFloat(tag, value.toFloat())
            DoubleRepresentation.LONG_BITS -> encodeTaggedLong(tag, value.toBits())
            DoubleRepresentation.STRING -> encodeTaggedString(tag, value.toString())
        }
    }

    override fun encodeTaggedChar(tag: String, value: Char) {
        encodeTaggedString(tag, value.toString())
    }

    override fun encodeTaggedString(tag: String, value: String) {
        editor.putString(tag, value)
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int,
    ): CompositeEncoder {
        if (preferences.configuration.shouldSerializeStringSet(descriptor)) {
            return PreferencesStringSetEncoder(preferences, editor, popTag())
        }
        if (collectionSize == 0) {
            encodeEmptyStructureStart(descriptor)
        }
        return super.beginCollection(descriptor, collectionSize)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (descriptor.elementsCount == 0 && descriptor.kind !is PrimitiveKind) {
            encodeEmptyStructureStart(descriptor)
        }
        return super.beginStructure(descriptor)
    }

    private fun encodeEmptyStructureStart(descriptor: SerialDescriptor) {
        if (preferences.configuration.encodeObjectStarts) {
            editor.putBoolean(currentTag, true)
        } else {
            throw SerializationException(
                "cannot encode empty structure ${descriptor.serialName} at $currentTag " +
                        "(use encodeObjectStarts=true on Preferences creation to change this behavior)"
            )
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal class PreferencesStringSetEncoder(
    private val preferences: Preferences,
    private val editor: SharedPreferences.Editor,
    private val currentTag: String,
) : AbstractEncoder() {

    override val serializersModule: SerializersModule get() = preferences.serializersModule
    private val setBuilder = mutableSetOf<String?>()

    override fun encodeValue(value: Any) {
        throw SerializationException("${value::class} encoding is not supported while encoding a string set")
    }

    override fun encodeNull() {
        setBuilder.add(null)
    }

    override fun encodeChar(value: Char) {
        setBuilder.add(value.toString())
    }

    override fun encodeString(value: String) {
        setBuilder.add(value)
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        setBuilder.add(enumDescriptor.getElementName(index))
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        editor.putStringSet(currentTag, setBuilder)
    }
}
