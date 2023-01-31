package dev.ragnarok.filegallery.util.serializeble.msgpack.internal

import dev.ragnarok.filegallery.util.serializeble.msgpack.exceptions.MsgPackSerializationException
import dev.ragnarok.filegallery.util.serializeble.msgpack.types.MsgPackType
import dev.ragnarok.filegallery.util.serializeble.msgpack.utils.splitToByteArray

internal interface MsgPacker {
    fun packNull(): ByteArray
    fun packBoolean(value: Boolean): ByteArray
    fun packByte(value: Byte, strict: Boolean = false): ByteArray
    fun packShort(value: Short, strict: Boolean = false): ByteArray
    fun packInt(value: Int, strict: Boolean = false): ByteArray
    fun packLong(value: Long, strict: Boolean = false): ByteArray
    fun packFloat(value: Float): ByteArray
    fun packDouble(value: Double): ByteArray
    fun packString(value: String, rawCompatibility: Boolean = false): ByteArray
    fun packByteArray(value: ByteArray): ByteArray
}

internal class BasicMsgPacker : MsgPacker {
    override fun packNull(): ByteArray {
        return byteArrayOf(MsgPackType.NULL)
    }

    override fun packBoolean(value: Boolean): ByteArray {
        return byteArrayOf(MsgPackType.Boolean(value))
    }

    override fun packByte(value: Byte, strict: Boolean): ByteArray {
        return if (value >= MsgPackType.Int.MIN_NEGATIVE_SINGLE_BYTE) {
            byteArrayOf(value)
        } else {
            byteArrayOf(MsgPackType.Int.INT8, value)
        }
    }

    override fun packShort(value: Short, strict: Boolean): ByteArray {
        return if (value in MsgPackType.Int.MIN_NEGATIVE_BYTE..Byte.MAX_VALUE && !strict) {
            packByte(value.toByte())
        } else {
            var uByte = false
            val type =
                when {
                    value < 0 -> MsgPackType.Int.INT16
                    value <= MsgPackType.Int.MAX_UBYTE && !strict -> MsgPackType.Int.UINT8.also {
                        uByte = true
                    }

                    else -> MsgPackType.Int.UINT16
                }
            if (uByte) {
                byteArrayOf(type, (value.toInt() and 0xff).toByte())
            } else {
                byteArrayOf(type) + value.splitToByteArray()
            }
        }
    }

    override fun packInt(value: Int, strict: Boolean): ByteArray {
        return if (value in Short.MIN_VALUE..Short.MAX_VALUE && !strict) {
            packShort(value.toShort())
        } else {
            var uShort = false
            val type =
                when {
                    value < 0 -> MsgPackType.Int.INT32
                    value <= MsgPackType.Int.MAX_USHORT && !strict -> MsgPackType.Int.UINT16.also {
                        uShort = true
                    }

                    else -> MsgPackType.Int.UINT32
                }
            if (uShort) {
                byteArrayOf(type) + value.toShort().splitToByteArray()
            } else {
                byteArrayOf(type) + value.splitToByteArray()
            }
        }
    }

    override fun packLong(value: Long, strict: Boolean): ByteArray {
        return if (value in Int.MIN_VALUE..Int.MAX_VALUE && !strict) {
            packInt(value.toInt())
        } else {
            var uInt = false
            val type =
                when {
                    value < 0 -> MsgPackType.Int.INT64
                    value <= MsgPackType.Int.MAX_UINT && !strict -> MsgPackType.Int.UINT32.also {
                        uInt = true
                    }

                    else -> MsgPackType.Int.UINT64
                }
            if (uInt) {
                byteArrayOf(type) + value.toInt().splitToByteArray()
            } else {
                byteArrayOf(type) + value.splitToByteArray()
            }
        }
    }

    override fun packFloat(value: Float): ByteArray {
        return byteArrayOf(MsgPackType.Float.FLOAT) + value.toRawBits().splitToByteArray()
    }

    override fun packDouble(value: Double): ByteArray {
        return byteArrayOf(MsgPackType.Float.DOUBLE) + value.toRawBits().splitToByteArray()
    }

    override fun packString(value: String, rawCompatibility: Boolean): ByteArray {
        val bytes = value.encodeToByteArray()
        val prefix = when {
            bytes.size <= MsgPackType.String.MAX_FIXSTR_LENGTH -> {
                byteArrayOf(MsgPackType.String.FIXSTR_SIZE_MASK.maskValue(bytes.size.toByte()))
            }

            bytes.size <= MsgPackType.String.MAX_STR8_LENGTH && !rawCompatibility -> {
                byteArrayOf(MsgPackType.String.STR8) + bytes.size.toByte().splitToByteArray()
            }

            bytes.size <= MsgPackType.String.MAX_STR16_LENGTH -> {
                byteArrayOf(MsgPackType.String.STR16) + bytes.size.toShort().splitToByteArray()
            }

            bytes.size <= MsgPackType.String.MAX_STR32_LENGTH -> {
                byteArrayOf(MsgPackType.String.STR32) + bytes.size.splitToByteArray()
            }

            else -> throw MsgPackSerializationException.packingError("String too long. Byte size: ${bytes.size}. Max size: ${MsgPackType.String.MAX_STR32_LENGTH}")
        }
        return prefix + bytes
    }

    override fun packByteArray(value: ByteArray): ByteArray {
        val prefix = when {
            value.size <= MsgPackType.Bin.MAX_BIN8_LENGTH -> {
                byteArrayOf(MsgPackType.Bin.BIN8) + value.size.toByte().splitToByteArray()
            }

            value.size <= MsgPackType.Bin.MAX_BIN16_LENGTH -> {
                byteArrayOf(MsgPackType.Bin.BIN16) + value.size.toShort().splitToByteArray()
            }

            value.size <= MsgPackType.Bin.MAX_BIN32_LENGTH -> {
                byteArrayOf(MsgPackType.Bin.BIN32) + value.size.splitToByteArray()
            }

            else -> throw MsgPackSerializationException.packingError("Byte array too long. Byte size: ${value.size}. Max size: ${MsgPackType.Bin.MAX_BIN32_LENGTH}")
        }
        return prefix + value
    }
}
