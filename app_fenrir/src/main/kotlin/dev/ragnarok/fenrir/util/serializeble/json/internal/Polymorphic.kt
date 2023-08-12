/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package dev.ragnarok.fenrir.util.serializeble.json.internal

import dev.ragnarok.fenrir.util.serializeble.json.Json
import dev.ragnarok.fenrir.util.serializeble.json.JsonClassDiscriminator
import dev.ragnarok.fenrir.util.serializeble.json.JsonDecoder
import dev.ragnarok.fenrir.util.serializeble.json.JsonEncoder
import dev.ragnarok.fenrir.util.serializeble.json.JsonObject
import dev.ragnarok.fenrir.util.serializeble.json.jsonPrimitive
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.findPolymorphicSerializer
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.internal.jsonCachedSerialNames

@OptIn(InternalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
internal inline fun <T> JsonEncoder.encodePolymorphically(
    serializer: SerializationStrategy<T>,
    value: T,
    ifPolymorphic: (String) -> Unit
) {
    if (serializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
        serializer.serialize(this, value)
        return
    }
    val casted = serializer as AbstractPolymorphicSerializer<Any>
    val baseClassDiscriminator = serializer.descriptor.classDiscriminator(json)
    val actualSerializer = casted.findPolymorphicSerializer(this, value as Any)
    validateIfSealed(casted, actualSerializer, baseClassDiscriminator)
    checkKind(actualSerializer.descriptor.kind)
    ifPolymorphic(baseClassDiscriminator)
    actualSerializer.serialize(this, value)
}

@OptIn(InternalSerializationApi::class)
private fun validateIfSealed(
    serializer: SerializationStrategy<*>,
    actualSerializer: SerializationStrategy<Any>,
    classDiscriminator: String
) {
    if (serializer !is SealedClassSerializer<*>) return
    @Suppress("DEPRECATION_ERROR")
    if (classDiscriminator in actualSerializer.descriptor.jsonCachedSerialNames()) {
        val baseName = serializer.descriptor.serialName
        val actualName = actualSerializer.descriptor.serialName
        error(
            "Sealed class '$actualName' cannot be serialized as base class '$baseName' because" +
                    " it has property name that conflicts with JSON class discriminator '$classDiscriminator'. " +
                    "You can either change class discriminator in JsonConfiguration, " +
                    "rename property with @SerialName annotation or fall back to array polymorphism"
        )
    }
}

internal fun checkKind(kind: SerialKind) {
    if (kind is SerialKind.ENUM) error("Enums cannot be serialized polymorphically with 'type' parameter. You can use 'JsonBuilder.useArrayPolymorphism' instead")
    if (kind is PrimitiveKind) error("Primitives cannot be serialized polymorphically with 'type' parameter. You can use 'JsonBuilder.useArrayPolymorphism' instead")
    if (kind is PolymorphicKind) error("Actual serializer for polymorphic cannot be polymorphic itself")
}

@OptIn(InternalSerializationApi::class)
internal fun <T> JsonDecoder.decodeSerializableValuePolymorphic(deserializer: DeserializationStrategy<T>): T {
    // NB: changes in this method should be reflected in StreamingJsonDecoder#decodeSerializableValue
    if (deserializer !is AbstractPolymorphicSerializer<*> || json.configuration.useArrayPolymorphism) {
        return deserializer.deserialize(this)
    }
    val discriminator = deserializer.descriptor.classDiscriminator(json)

    val jsonTree = cast<JsonObject>(decodeJsonElement(), deserializer.descriptor)
    val type = jsonTree[discriminator]?.jsonPrimitive?.content
    val actualSerializer = deserializer.findPolymorphicSerializerOrNull(this, type)
        ?: throwSerializerNotFound(type, jsonTree)

    @Suppress("UNCHECKED_CAST")
    return json.readPolymorphicJson(
        discriminator,
        jsonTree,
        actualSerializer as DeserializationStrategy<T>
    )
}

@JvmName("throwSerializerNotFound")
internal fun throwSerializerNotFound(type: String?, jsonTree: JsonObject): Nothing {
    val suffix =
        if (type == null) "missing class discriminator ('null')"
        else "class discriminator '$type'"
    throw JsonDecodingException(
        -1,
        "Polymorphic serializer was not found for $suffix",
        jsonTree.toString()
    )
}

internal fun SerialDescriptor.classDiscriminator(json: Json): String {
    // Plain loop is faster than allocation of Sequence or ArrayList
    // We can rely on the fact that only one JsonClassDiscriminator is present —
    // compiler plugin checked that.
    for (annotation in annotations) {
        if (annotation is JsonClassDiscriminator) return annotation.discriminator
    }
    return json.configuration.classDiscriminator
}

