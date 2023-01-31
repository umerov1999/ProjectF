package dev.ragnarok.fenrir.util.serializeble.msgpack.internal

import dev.ragnarok.fenrir.util.serializeble.msgpack.exceptions.MsgPackSerializationException
import dev.ragnarok.fenrir.util.serializeble.msgpack.stream.MsgPackDataInputBuffer
import dev.ragnarok.fenrir.util.serializeble.msgpack.types.MsgPackType
import dev.ragnarok.fenrir.util.serializeble.msgpack.utils.joinToNumber

internal interface MsgUnpacker {
    fun unpackNull()
    fun unpackBoolean(): Boolean
    fun unpackByte(strict: Boolean = false, preventOverflow: Boolean = false): Byte
    fun unpackShort(strict: Boolean = false, preventOverflow: Boolean = false): Short
    fun unpackInt(strict: Boolean = false, preventOverflow: Boolean = false): Int
    fun unpackLong(strict: Boolean = false, preventOverflow: Boolean = false): Long
    fun unpackFloat(strict: Boolean = false): Float
    fun unpackDouble(strict: Boolean = false): Double
    fun unpackString(preventOverflow: Boolean = false): String
    fun unpackByteArray(preventOverflow: Boolean = false): ByteArray
}

internal class BasicMsgUnpacker(private val dataBuffer: MsgPackDataInputBuffer) : MsgUnpacker {
    override fun unpackNull() {
        val next = dataBuffer.requireNextByte()
        if (next != MsgPackType.NULL) throw Exception("Invalid null $next")
    }

    override fun unpackBoolean(): Boolean {
        return when (val next = dataBuffer.requireNextByte()) {
            MsgPackType.Boolean.TRUE -> true
            MsgPackType.Boolean.FALSE -> false
            else -> throw Exception("Invalid boolean $next")
        }
    }

    override fun unpackByte(strict: Boolean, preventOverflow: Boolean): Byte {
        // Check is it a single byte value
        val next = dataBuffer.peek()
        return when {
            MsgPackType.Int.POSITIVE_FIXNUM_MASK.test(next) or MsgPackType.Int.NEGATIVE_FIXNUM_MASK.test(
                next
            ) -> {
                dataBuffer.requireNextByte()
                next
            }

            MsgPackType.Int.isByte(next) -> {
                dataBuffer.requireNextByte()
                if (next == MsgPackType.Int.UINT8 && preventOverflow) {
                    val number = (dataBuffer.requireNextByte().toInt() and 0xff).toShort()
                    if (number !in Byte.MIN_VALUE..Byte.MAX_VALUE) {
                        throw MsgPackSerializationException.overflowError(dataBuffer)
                    } else {
                        number.toByte()
                    }
                } else {
                    dataBuffer.requireNextByte()
                }
            }

            else -> if (strict) throw MsgPackSerializationException.deserialization(
                dataBuffer,
                "Expected byte type, but found $next"
            ) else unpackNumber(preventOverflow).toByte()
        }
    }

    override fun unpackShort(strict: Boolean, preventOverflow: Boolean): Short {
        val next = dataBuffer.peek()
        return when {
            MsgPackType.Int.isShort(next) -> {
                dataBuffer.skip(1)
                if (next == MsgPackType.Int.UINT16 && preventOverflow) {
                    val number = dataBuffer.takeNext(2).joinToNumber<Int>()
                    if (number !in Short.MIN_VALUE..Short.MAX_VALUE) {
                        throw MsgPackSerializationException.overflowError(dataBuffer)
                    } else {
                        number.toShort()
                    }
                } else {
                    dataBuffer.takeNext(2).joinToNumber()
                }
            }

            next == MsgPackType.Int.UINT8 -> {
                dataBuffer.skip(1)
                (dataBuffer.requireNextByte().toInt() and 0xff).toShort()
            }

            else -> if (strict) throw MsgPackSerializationException.deserialization(
                dataBuffer,
                "Expected short type, but found $next"
            ) else unpackNumber(preventOverflow).toShort()
        }
    }

    override fun unpackInt(strict: Boolean, preventOverflow: Boolean): Int {
        val next = dataBuffer.peek()
        return when {
            MsgPackType.Int.isInt(next) -> {
                dataBuffer.skip(1)
                if (next == MsgPackType.Int.UINT32 && preventOverflow) {
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

            next == MsgPackType.Int.UINT16 -> {
                dataBuffer.skip(1)
                dataBuffer.takeNext(2).joinToNumber()
            }

            else -> if (strict) throw MsgPackSerializationException.deserialization(
                dataBuffer,
                "Expected int type, but found $next"
            ) else unpackNumber(preventOverflow).toInt()
        }
    }

    override fun unpackLong(strict: Boolean, preventOverflow: Boolean): Long {
        val next = dataBuffer.peek()
        return when {
            MsgPackType.Int.isLong(next) -> {
                dataBuffer.skip(1)
                if (next == MsgPackType.Int.UINT64 && preventOverflow) {
                    val number = dataBuffer.takeNext(8).joinToNumber<Long>()
                    if (number < 0) {
                        throw MsgPackSerializationException.overflowError(dataBuffer)
                    } else {
                        number
                    }
                } else {
                    dataBuffer.takeNext(8).joinToNumber()
                }
            }

            next == MsgPackType.Int.UINT32 -> {
                dataBuffer.skip(1)
                dataBuffer.takeNext(4).joinToNumber()
            }

            else -> if (strict) throw MsgPackSerializationException.deserialization(
                dataBuffer,
                "Expected long type, but found $next"
            ) else unpackNumber(preventOverflow).toLong()
        }
    }

    override fun unpackFloat(strict: Boolean): Float {
        return when (val type = dataBuffer.peek()) {
            MsgPackType.Float.FLOAT -> {
                dataBuffer.skip(1)
                Float.fromBits(dataBuffer.takeNext(4).joinToNumber())
            }

            else -> if (strict) throw MsgPackSerializationException.deserialization(
                dataBuffer,
                "Expected float type, but found $type"
            ) else unpackNumber().toFloat()
        }
    }

    override fun unpackDouble(strict: Boolean): Double {
        return when (val type = dataBuffer.peek()) {
            MsgPackType.Float.DOUBLE -> {
                dataBuffer.skip(1)
                Double.fromBits(dataBuffer.takeNext(8).joinToNumber())
            }

            else -> if (strict) throw MsgPackSerializationException.deserialization(
                dataBuffer,
                "Expected double type, but found $type"
            ) else unpackNumber().toDouble()
        }
    }

    override fun unpackString(preventOverflow: Boolean): String {
        val next = dataBuffer.requireNextByte()
        val length = when {
            MsgPackType.String.FIXSTR_SIZE_MASK.test(next) -> MsgPackType.String.FIXSTR_SIZE_MASK.unMaskValue(
                next
            ).toInt()

            next == MsgPackType.String.STR8 -> dataBuffer.requireNextByte().toInt() and 0xff
            next == MsgPackType.String.STR16 -> dataBuffer.takeNext(2).joinToNumber()
            next == MsgPackType.String.STR32 -> {
                if (preventOverflow) {
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
                    "Expected string type, but found $next"
                )
            }
        }
        if (length == 0) return ""
        return dataBuffer.takeNext(length).decodeToString()
    }

    override fun unpackByteArray(preventOverflow: Boolean): ByteArray {
        val length = when (val next = dataBuffer.requireNextByte()) {
            MsgPackType.Bin.BIN8 -> dataBuffer.requireNextByte().toInt() and 0xff
            MsgPackType.Bin.BIN16 -> dataBuffer.takeNext(2).joinToNumber()
            MsgPackType.Bin.BIN32 -> {
                if (preventOverflow) {
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
                    "Expected binary type, but found $next"
                )
            }
        }
        if (length == 0) return byteArrayOf()
        return dataBuffer.takeNext(length)
    }

    private fun unpackNumber(preventOverflow: Boolean = false): Number {
        val type = dataBuffer.peek()
        return when {
            MsgPackType.Int.POSITIVE_FIXNUM_MASK.test(type) ||
                    MsgPackType.Int.NEGATIVE_FIXNUM_MASK.test(type) ||
                    type == MsgPackType.Int.INT8 -> unpackByte(true, preventOverflow)

            type == MsgPackType.Int.INT16 || type == MsgPackType.Int.UINT8 -> {
                val result = unpackShort(true, preventOverflow)
                if (type == MsgPackType.Int.UINT8 && result <= Byte.MAX_VALUE && result >= Byte.MIN_VALUE) {
                    result.toByte()
                } else {
                    result
                }
            }

            type == MsgPackType.Int.INT32 || type == MsgPackType.Int.UINT16 -> {
                val result = unpackInt(true, preventOverflow)
                if (type == MsgPackType.Int.UINT16 && result <= Short.MAX_VALUE && result >= Short.MIN_VALUE) {
                    result.toShort()
                } else {
                    result
                }
            }

            type == MsgPackType.Int.INT64 || type == MsgPackType.Int.UINT32 || type == MsgPackType.Int.UINT64 -> {
                val result = unpackLong(true, preventOverflow)
                if (type == MsgPackType.Int.UINT32 && result <= Int.MAX_VALUE && result >= Int.MIN_VALUE) {
                    result.toInt()
                } else {
                    result
                }
            }

            type == MsgPackType.Float.FLOAT -> unpackFloat(true)
            type == MsgPackType.Float.DOUBLE -> unpackDouble(true)
            else -> throw MsgPackSerializationException.deserialization(
                dataBuffer,
                "Expected binary type, but found $type"
            )
        }
    }
}
