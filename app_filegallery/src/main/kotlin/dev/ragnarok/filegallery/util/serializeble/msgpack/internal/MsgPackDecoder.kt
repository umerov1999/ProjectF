package dev.ragnarok.filegallery.util.serializeble.msgpack.internal

import dev.ragnarok.filegallery.util.serializeble.json.JsonElement
import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPackConfiguration
import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPackNullableDynamicSerializer
import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPackTreeReader
import dev.ragnarok.filegallery.util.serializeble.msgpack.exceptions.MsgPackSerializationException
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataInputBuffer
import dev.ragnarok.filegallery.util.serializeble.msgpack.types.MsgPackType
import dev.ragnarok.filegallery.util.serializeble.msgpack.utils.joinToNumber
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

interface MsgPackTypeDecoder {
    fun peekNextType(): Byte
}

internal class BasicMsgPackDecoder(
    val configuration: MsgPackConfiguration,
    override val serializersModule: SerializersModule,
    val dataBuffer: MsgPackDataInputBuffer,
    private val msgUnpacker: MsgUnpacker = BasicMsgUnpacker(dataBuffer),
    val inlineDecoders: Map<SerialDescriptor, (InlineDecoderHelper) -> Decoder> = mapOf()
) : AbstractDecoder(), MsgPackTypeDecoder {
    fun decodeMsgPackElement(): JsonElement = MsgPackTreeReader(this).read()

    val depthStack: ArrayDeque<Unit> = ArrayDeque()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (descriptor.kind in arrayOf(StructureKind.CLASS, StructureKind.OBJECT)) {
            val next = dataBuffer.peekSafely()
            if (next != null && MsgPackType.String.isString(next)) {
                val fieldName = kotlin.runCatching { decodeString() }.getOrNull()
                    ?: return CompositeDecoder.UNKNOWN_NAME
                val index = descriptor.getElementIndex(fieldName)
                return if (index == CompositeDecoder.UNKNOWN_NAME && configuration.ignoreUnknownKeys && depthStack.isEmpty()) {
                    MsgPackNullableDynamicSerializer.deserialize(this)
                    decodeElementIndex(descriptor)
                } else {
                    index
                }
            } else {
                return CompositeDecoder.DECODE_DONE
            }
        }
        return 0
    }

    override fun decodeSequentially(): Boolean = true

    override fun decodeNotNullMark(): Boolean {
        val next = dataBuffer.peek()
        return next != MsgPackType.NULL
    }

    override fun decodeNull(): Nothing? {
        msgUnpacker.unpackNull()
        return null
    }

    override fun decodeBoolean(): Boolean {
        val token = dataBuffer.peek()
        if (MsgPackType.Int.isInt(token) || MsgPackType.Int.POSITIVE_FIXNUM_MASK.test(token) or MsgPackType.Int.NEGATIVE_FIXNUM_MASK.test(
                token
            )
        ) {
            return decodeInt() > 0
        }
        return msgUnpacker.unpackBoolean()
    }

    override fun decodeByte(): Byte {
        return msgUnpacker.unpackByte(configuration.strictTypes, configuration.preventOverflows)
    }

    override fun decodeShort(): Short {
        return msgUnpacker.unpackShort(configuration.strictTypes, configuration.preventOverflows)
    }

    override fun decodeInt(): Int {
        return msgUnpacker.unpackInt(configuration.strictTypes, configuration.preventOverflows)
    }

    override fun decodeLong(): Long {
        return msgUnpacker.unpackLong(configuration.strictTypes, configuration.preventOverflows)
    }

    override fun decodeFloat(): Float {
        return msgUnpacker.unpackFloat(configuration.strictTypes)
    }

    override fun decodeDouble(): Double {
        return msgUnpacker.unpackDouble(configuration.strictTypes)
    }

    override fun decodeString(): String {
        val token = dataBuffer.peek()
        if (MsgPackType.String.isString(token)) {
            return msgUnpacker.unpackString()
        }
        return decodeLong().toString()
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        return enumDescriptor.getElementIndex(decodeString())
    }

    fun decodeByteArray(): ByteArray {
        return if (configuration.rawCompatibility) {
            val next = dataBuffer.peek()
            if (MsgPackType.String.FIXSTR_SIZE_MASK.test(next) ||
                next == MsgPackType.String.STR16 ||
                next == MsgPackType.String.STR32
            ) {
                msgUnpacker.unpackString().encodeToByteArray()
            } else {
                msgUnpacker.unpackByteArray()
            }
        } else {
            msgUnpacker.unpackByteArray()
        }
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        if (inlineDecoders.containsKey(descriptor)) {
            return inlineDecoders[descriptor]!!(
                InlineDecoderHelper(
                    serializersModule,
                    dataBuffer
                )
            )
        }
        return super.decodeInline(descriptor)
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        val next = dataBuffer.requireNextByte()
        return when (descriptor.kind) {
            StructureKind.LIST ->
                when {
                    MsgPackType.Array.FIXARRAY_SIZE_MASK.test(next) -> MsgPackType.Array.FIXARRAY_SIZE_MASK.unMaskValue(
                        next
                    ).toInt()

                    next == MsgPackType.Array.ARRAY16 -> dataBuffer.takeNext(2).joinToNumber()
                    next == MsgPackType.Array.ARRAY32 -> {
                        if (configuration.preventOverflows) {
                            val number = dataBuffer.takeNext(4).joinToNumber<Long>()
                            if (number !in Int.MIN_VALUE..Int.MAX_VALUE) {
                                throw MsgPackSerializationException.overflowError(dataBuffer)
                            } else {
                                number.toInt()
                            }
                        } else {
                            dataBuffer.takeNext(4).joinToNumber()
                        }
                    }

                    else -> {
                        throw MsgPackSerializationException.deserialization(
                            dataBuffer,
                            "Unknown array type: $next"
                        )
                    }
                }

            StructureKind.CLASS, StructureKind.OBJECT, StructureKind.MAP ->
                when {
                    MsgPackType.Map.FIXMAP_SIZE_MASK.test(next) -> MsgPackType.Map.FIXMAP_SIZE_MASK.unMaskValue(
                        next
                    ).toInt()

                    next == MsgPackType.Map.MAP16 -> dataBuffer.takeNext(2).joinToNumber()
                    next == MsgPackType.Map.MAP32 -> {
                        if (configuration.preventOverflows) {
                            val number = dataBuffer.takeNext(4).joinToNumber<Long>()
                            if (number !in Int.MIN_VALUE..Int.MAX_VALUE) {
                                throw MsgPackSerializationException.overflowError(dataBuffer)
                            } else {
                                number.toInt()
                            }
                        } else {
                            dataBuffer.takeNext(4).joinToNumber()
                        }
                    }

                    else -> {
                        throw MsgPackSerializationException.deserialization(
                            dataBuffer,
                            "Unknown map type: $next"
                        )
                    }
                }

            else -> {
                throw MsgPackSerializationException.deserialization(
                    dataBuffer,
                    "Unsupported collection: ${descriptor.kind}"
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        return if (deserializer == ByteArraySerializer()) {
            decodeByteArray() as T
        } else {
            super.decodeSerializableValue(deserializer)
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (descriptor.kind in arrayOf(StructureKind.CLASS, StructureKind.OBJECT)) {
            if (descriptor.serialName == "dev.ragnarok.filegallery.util.serializeble.msgpack.extensions.MsgPackExtension") {
                return ExtensionTypeDecoder(this)
            }

            depthStack.addFirst(Unit)
            // Handle extension types as arrays
            val size = decodeCollectionSize(descriptor)
            return ClassMsgPackDecoder(this, size)
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        super.endStructure(descriptor)
        depthStack.removeFirstOrNull()
    }

    override fun peekNextType(): Byte {
        return dataBuffer.peek()
    }
}

internal class MsgPackDecoder(
    private val basicMsgPackDecoder: BasicMsgPackDecoder
) : Decoder by basicMsgPackDecoder, CompositeDecoder by basicMsgPackDecoder,
    MsgPackTypeDecoder by basicMsgPackDecoder {
    override val serializersModule: SerializersModule = basicMsgPackDecoder.serializersModule
}

internal class ClassMsgPackDecoder(
    private val basicMsgPackDecoder: BasicMsgPackDecoder,
    private val size: Int
) : Decoder by basicMsgPackDecoder, CompositeDecoder by basicMsgPackDecoder,
    MsgPackTypeDecoder by basicMsgPackDecoder {
    override val serializersModule: SerializersModule = basicMsgPackDecoder.serializersModule
    private fun decodeElemIndex(descriptor: SerialDescriptor, size: Int): Int {
        if (descriptor.kind in arrayOf(StructureKind.CLASS, StructureKind.OBJECT)) {
            val next = basicMsgPackDecoder.dataBuffer.peekSafely()
            if (next != null && MsgPackType.String.isString(next)) {
                val fieldName = kotlin.runCatching { decodeString() }.getOrNull()
                    ?: return CompositeDecoder.UNKNOWN_NAME
                val index = descriptor.getElementIndex(fieldName)
                return if (index == CompositeDecoder.UNKNOWN_NAME && basicMsgPackDecoder.configuration.ignoreUnknownKeys && basicMsgPackDecoder.depthStack.isEmpty()) {
                    MsgPackNullableDynamicSerializer.deserialize(this)
                    decodedElements++
                    if (decodedElements >= size) CompositeDecoder.DECODE_DONE else decodeElemIndex(
                        descriptor,
                        size
                    )
                } else {
                    index
                }
            } else {
                return CompositeDecoder.DECODE_DONE
            }
        }
        return 0
    }

    private var decodedElements = 0

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (decodedElements >= size) return CompositeDecoder.DECODE_DONE
        val result = decodeElemIndex(descriptor, size)
        if (result != CompositeDecoder.DECODE_DONE) decodedElements++
        return if (result == CompositeDecoder.UNKNOWN_NAME) {
            MsgPackNullableDynamicSerializer.deserialize(this)
            decodeElementIndex(descriptor)
        } else {
            result
        }
    }

    override fun decodeSequentially(): Boolean = false
}

internal class ExtensionTypeDecoder(
    private val basicMsgPackDecoder: BasicMsgPackDecoder
) : CompositeDecoder, AbstractDecoder(), MsgPackTypeDecoder by basicMsgPackDecoder {
    private val dataBuffer = basicMsgPackDecoder.dataBuffer
    var type: Byte? = null
    var typeId: Byte? = null
    var size: Int? = null
    var bytesRead = 0

    override val serializersModule: SerializersModule = basicMsgPackDecoder.serializersModule

    override fun decodeSequentially(): Boolean = false

    override fun decodeByte(): Byte {
        return if (bytesRead == 0) {
            val byte = dataBuffer.requireNextByte()
            bytesRead++
            if (!MsgPackType.Ext.isExt(byte)) {
                throw MsgPackSerializationException.deserialization(
                    dataBuffer,
                    "Unexpected byte: $byte. Expected extension type byte!"
                )
            }
            type = byte
            if (MsgPackType.Ext.SIZES.containsKey(type)) {
                size = MsgPackType.Ext.SIZES[type]
            }
            type!!
        } else if (bytesRead == 1 && size != null) {
            val byte = dataBuffer.requireNextByte()
            bytesRead++
            typeId = byte
            typeId!!
        } else if (bytesRead == 1 && size == null) {
            val sizeSize = MsgPackType.Ext.SIZE_SIZE[type]
            bytesRead += sizeSize!!
            size = dataBuffer.takeNext(sizeSize).joinToNumber()
            val byte = dataBuffer.requireNextByte()
            typeId = byte
            typeId!!
        } else {
            throw AssertionError()
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return if (bytesRead <= 2) bytesRead else CompositeDecoder.DECODE_DONE
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return size ?: 0
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        bytesRead += 1
        return dataBuffer.takeNext(
            size ?: 0
        ) as T
    }
}

data class InlineDecoderHelper(
    val serializersModule: SerializersModule,
    val inputBuffer: MsgPackDataInputBuffer
)
