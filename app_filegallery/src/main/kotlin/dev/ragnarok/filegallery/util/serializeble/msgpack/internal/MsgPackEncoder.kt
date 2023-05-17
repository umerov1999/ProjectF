package dev.ragnarok.filegallery.util.serializeble.msgpack.internal

import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPackConfiguration
import dev.ragnarok.filegallery.util.serializeble.msgpack.exceptions.MsgPackSerializationException
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataOutputArrayBuffer
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataOutputArrayBufferCompressed
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataOutputBuffer
import dev.ragnarok.filegallery.util.serializeble.msgpack.types.MsgPackType
import dev.ragnarok.filegallery.util.serializeble.msgpack.utils.splitToByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

internal class BasicMsgPackEncoder(
    private val configuration: MsgPackConfiguration,
    override val serializersModule: SerializersModule,
    private val packer: MsgPacker = BasicMsgPacker(),
    compressed: Boolean,
    val inlineEncoders: Map<SerialDescriptor, (InlineEncoderHelper) -> Encoder> = mapOf()
) : AbstractEncoder() {
    val result: MsgPackDataOutputBuffer =
        if (compressed) MsgPackDataOutputArrayBufferCompressed() else MsgPackDataOutputArrayBuffer()

    override fun encodeBoolean(value: Boolean) {
        result.addAll(packer.packBoolean(value))
    }

    override fun encodeNull() {
        result.addAll(packer.packNull())
    }

    override fun encodeByte(value: Byte) {
        result.addAll(packer.packByte(value, configuration.strictTypeWriting))
    }

    override fun encodeShort(value: Short) {
        result.addAll(packer.packShort(value, configuration.strictTypeWriting))
    }

    override fun encodeInt(value: Int) {
        result.addAll(packer.packInt(value, configuration.strictTypeWriting))
    }

    override fun encodeLong(value: Long) {
        result.addAll(packer.packLong(value, configuration.strictTypeWriting))
    }

    override fun encodeFloat(value: Float) {
        result.addAll(packer.packFloat(value))
    }

    override fun encodeDouble(value: Double) {
        result.addAll(packer.packDouble(value))
    }

    override fun encodeString(value: String) {
        result.addAll(packer.packString(value, configuration.rawCompatibility))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        if (configuration.ordinalEnums) {
            result.addAll(packer.packInt(index, configuration.strictTypeWriting))
        } else {
            result.addAll(
                packer.packString(
                    enumDescriptor.getElementName(index),
                    configuration.rawCompatibility
                )
            )
        }
    }

    fun encodeByteArray(value: ByteArray) {
        if (configuration.rawCompatibility) {
            result.addAll(packer.packString(value.decodeToString(), true))
        } else {
            result.addAll(packer.packByteArray(value))
        }
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        if (inlineEncoders.containsKey(descriptor)) {
            return inlineEncoders[descriptor]!!(
                InlineEncoderHelper(
                    serializersModule,
                    result
                )
            )
        }
        return super.encodeInline(descriptor)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return if (descriptor.kind in arrayOf(StructureKind.CLASS, StructureKind.OBJECT)) {
            if (descriptor.serialName == "dev.ragnarok.filegallery.util.serializeble.msgpack.extensions.MsgPackExtension") {
                ExtensionTypeEncoder(this)
            } else {
                beginCollection(descriptor, descriptor.elementsCount)
                MsgPackClassEncoder(this)
            }
        } else {
            this
        }
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int
    ): CompositeEncoder {
        when (descriptor.kind) {
            StructureKind.LIST ->
                when {
                    collectionSize <= MsgPackType.Array.MAX_FIXARRAY_SIZE -> {
                        result.add(MsgPackType.Array.FIXARRAY_SIZE_MASK.maskValue(collectionSize.toByte()))
                    }

                    collectionSize <= MsgPackType.Array.MAX_ARRAY16_LENGTH -> {
                        result.add(MsgPackType.Array.ARRAY16)
                        result.addAll(collectionSize.toShort().splitToByteArray())
                    }

                    collectionSize <= MsgPackType.Array.MAX_ARRAY32_LENGTH -> {
                        result.add(MsgPackType.Array.ARRAY32)
                        result.addAll(collectionSize.splitToByteArray())
                    }

                    else -> throw MsgPackSerializationException.serialization(
                        result,
                        "Collection too long (max size = ${MsgPackType.Array.MAX_ARRAY32_LENGTH}, size = $collectionSize)!"
                    )
                }

            StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP ->
                when {
                    collectionSize <= MsgPackType.Map.MAX_FIXMAP_SIZE -> {
                        result.add(MsgPackType.Map.FIXMAP_SIZE_MASK.maskValue(collectionSize.toByte()))
                    }

                    collectionSize <= MsgPackType.Map.MAX_MAP16_LENGTH -> {
                        result.add(MsgPackType.Map.MAP16)
                        result.addAll(collectionSize.toShort().splitToByteArray())
                    }

                    collectionSize <= MsgPackType.Map.MAX_MAP32_LENGTH -> {
                        result.add(MsgPackType.Map.MAP32)
                        result.addAll(collectionSize.splitToByteArray())
                    }

                    else -> throw MsgPackSerializationException.serialization(
                        result,
                        "Object too long (max size = ${MsgPackType.Map.MAX_MAP32_LENGTH}, size = $collectionSize)!"
                    )
                }

            else -> throw MsgPackSerializationException.serialization(
                result,
                "Unsupported collection type: ${descriptor.kind}"
            )
        }
        return this
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer == ByteArraySerializer()) {
            encodeByteArray(value as ByteArray)
        } else {
            super.encodeSerializableValue(serializer, value)
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // no-op, everything is handled when starting structure/collection
    }
}

internal class MsgPackEncoder(
    private val basicMsgPackEncoder: BasicMsgPackEncoder
) : Encoder by basicMsgPackEncoder, CompositeEncoder by basicMsgPackEncoder {
    override val serializersModule: SerializersModule = basicMsgPackEncoder.serializersModule
    val result = basicMsgPackEncoder.result
}

internal class ExtensionTypeEncoder(
    basicMsgPackEncoder: BasicMsgPackEncoder
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = basicMsgPackEncoder.serializersModule
    val result = basicMsgPackEncoder.result

    // TODO refactor
    private var bytesWritten = 0
    private var type: Byte? = null
    private var size: Int? = null
    private var typeId: Byte? = null

    override fun encodeByte(value: Byte) {
        if (bytesWritten == 0) {
            result.add(value)
            type = value
        } else if (bytesWritten == 1) {
            if (MsgPackType.Ext.SIZES.containsKey(type)) {
                result.add(value)
                size = MsgPackType.Ext.SIZES[type]
            }
            typeId = value
        }
        bytesWritten += 1
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        val valueByteArray = value as ByteArray
        if (size == null) {
            size = valueByteArray.size
            val maxSize = when (type) {
                MsgPackType.Ext.EXT8 -> MsgPackType.Ext.MAX_EXT8_LENGTH
                MsgPackType.Ext.EXT16 -> MsgPackType.Ext.MAX_EXT16_LENGTH
                MsgPackType.Ext.EXT32 -> MsgPackType.Ext.MAX_EXT32_LENGTH
                else -> throw MsgPackSerializationException.serialization(
                    result,
                    "Unexpected extension type: $type"
                )
            }.toLong()
            if ((size
                    ?: return).toLong() > maxSize
            ) throw MsgPackSerializationException.serialization(
                result,
                "Size ($size) too long for extension type ($maxSize)!"
            )
            result.addAll(
                when (type) {
                    MsgPackType.Ext.EXT8 -> (size ?: return).toByte().splitToByteArray()
                    MsgPackType.Ext.EXT16 -> (size ?: return).toShort().splitToByteArray()
                    MsgPackType.Ext.EXT32 -> (size ?: return).toInt().splitToByteArray()
                    else -> throw MsgPackSerializationException.serialization(
                        result,
                        "Unexpected extension type: $type"
                    )
                }
            )
            result.add(typeId ?: return)
        } else {
            if (valueByteArray.size != size) throw MsgPackSerializationException.serialization(
                result,
                "Invalid size for fixed size extension type! Expected $size but found ${valueByteArray.size}"
            )
        }
        result.addAll(valueByteArray)
    }
}

internal class MsgPackClassEncoder(
    private val basicMsgPackEncoder: BasicMsgPackEncoder
) : Encoder by basicMsgPackEncoder, CompositeEncoder by basicMsgPackEncoder {
    override val serializersModule: SerializersModule = basicMsgPackEncoder.serializersModule
    val result = basicMsgPackEncoder.result

    private fun encodeName(descriptor: SerialDescriptor, index: Int) {
        encodeString(descriptor.getElementName(index))
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        encodeName(descriptor, index)
        encodeBoolean(value)
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        encodeName(descriptor, index)
        encodeByte(value)
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        encodeName(descriptor, index)
        encodeChar(value)
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        encodeName(descriptor, index)
        encodeDouble(value)
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        encodeName(descriptor, index)
        encodeFloat(value)
    }

    @ExperimentalSerializationApi
    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
        encodeName(descriptor, index)
        return basicMsgPackEncoder.encodeInline(descriptor)
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        encodeName(descriptor, index)
        encodeInt(value)
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        encodeName(descriptor, index)
        encodeLong(value)
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        encodeName(descriptor, index)
        encodeShort(value)
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        encodeName(descriptor, index)
        encodeString(value)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // No-op
    }

    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        encodeName(descriptor, index)
        encodeNullableSerializableValue(serializer, value)
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        encodeName(descriptor, index)
        encodeSerializableValue(serializer, value)
    }
}

data class InlineEncoderHelper(
    val serializersModule: SerializersModule,
    val outputBuffer: MsgPackDataOutputBuffer
)
