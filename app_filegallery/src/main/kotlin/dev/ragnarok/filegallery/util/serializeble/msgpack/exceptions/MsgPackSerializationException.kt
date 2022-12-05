package dev.ragnarok.filegallery.util.serializeble.msgpack.exceptions

import dev.ragnarok.filegallery.util.serializeble.msgpack.extensions.MsgPackExtension
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataBuffer
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataInputBuffer
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataInputOkio
import dev.ragnarok.filegallery.util.serializeble.msgpack.stream.MsgPackDataOutputBuffer
import kotlinx.serialization.SerializationException

private fun ByteArray.toHex() = this.joinToString(separator = "") { it.toHex() }
private fun Byte.toHex() = toInt().and(0xff).toString(16).padStart(2, '0')
private fun MsgPackExtension.toInfoString() =
    "{type = $type, extTypeId = $extTypeId, data = ${data.toHex()}}"

class MsgPackSerializationException private constructor(
    override val message: String
) : SerializationException() {
    companion object {
        private fun MsgPackDataInputBuffer.locationInfo(): String {
            if (this is MsgPackDataInputOkio) {
                return currentIndex().toString() + " pos"
            }
            return toByteArray().toList().let {
                """
                ${
                    it.subList(0, currentIndex()).toByteArray().toHex()
                }[${peekSafely()}]${it.subList((currentIndex() + 1).coerceAtMost(it.size), it.size)}
                ${(0 until currentIndex()).joinToString(separator = "") { "  " }}} ^^ ${
                    ((currentIndex() + 1) until it.size).joinToString(
                        separator = ""
                    ) { "  " }
                }
                """.trimIndent()
            }
        }

        private fun MsgPackDataOutputBuffer.locationInfo() =
            "Written so far: ${toByteArray().toHex()}\nSize: ${toByteArray().size} bytes"

        private fun coreSerialization(
            buffer: MsgPackDataBuffer,
            locationInfo: String,
            reason: String? = null
        ): MsgPackSerializationException {
            val buf = (if (buffer is MsgPackDataInputOkio) buffer.currentIndex()
                .toString() + " pos" else buffer.toByteArray().toHex())
            return MsgPackSerializationException(
                "MsgPack Serialization failure while serializing: ${
                    buf
                }\nReason: $reason\nCurrent position:\n\n$locationInfo"
            )
        }

        private fun extensionSerialization(
            extension: MsgPackExtension,
            reason: String? = null
        ): MsgPackSerializationException {
            return MsgPackSerializationException(
                "MsgPack Serialization failure while serializing: ${extension.toInfoString()}\nReason: $reason"
            )
        }

        fun deserialization(
            buffer: MsgPackDataInputBuffer,
            reason: String? = null
        ): MsgPackSerializationException {
            return coreSerialization(buffer, buffer.locationInfo(), reason)
        }

        fun serialization(
            buffer: MsgPackDataOutputBuffer,
            reason: String? = null
        ): MsgPackSerializationException {
            return coreSerialization(buffer, buffer.locationInfo(), reason)
        }

        fun extensionSerializationWrongType(
            extension: MsgPackExtension,
            expectedType: Byte,
            foundType: Byte
        ): MsgPackSerializationException {
            return extensionSerialization(
                extension,
                "Expected extension type ${expectedType.toHex()} but found ${foundType.toHex()}. Deserialized extension: $extension"
            )
        }

        fun extensionDeserializationWrongType(
            extension: MsgPackExtension,
            expectedType: Byte,
            foundType: Byte
        ): MsgPackSerializationException {
            return extensionSerialization(
                extension,
                "Expected extension type ${expectedType.toHex()} but found ${foundType.toHex()}. Serialized extension: $extension"
            )
        }

        fun genericExtensionError(
            extension: MsgPackExtension,
            reason: String? = null
        ): MsgPackSerializationException {
            return extensionSerialization(extension, reason)
        }

        fun packingError(reason: String? = null): MsgPackSerializationException {
            return MsgPackSerializationException("MsgPack Serialization failure while packing value! Reason: $reason")
        }

        fun unpackingError(reason: String? = null): MsgPackSerializationException {
            return MsgPackSerializationException("MsgPack Serialization failure while unpacking value! Reason: $reason")
        }

        fun dynamicSerializationError(reason: String? = null): MsgPackSerializationException {
            return MsgPackSerializationException("MsgPack Dynamic Serialization failure! Reason: $reason")
        }

        fun strictTypeError(
            buffer: MsgPackDataInputBuffer,
            expectedType: String,
            foundType: String
        ): MsgPackSerializationException {
            return deserialization(
                buffer,
                "Strict type error! Expected type $expectedType, but found $foundType"
            )
        }

        fun overflowError(buffer: MsgPackDataInputBuffer): MsgPackSerializationException {
            return deserialization(buffer, "Overflow error!")
        }
    }
}
