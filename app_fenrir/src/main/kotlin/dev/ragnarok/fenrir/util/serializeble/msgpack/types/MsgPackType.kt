package dev.ragnarok.fenrir.util.serializeble.msgpack.types

internal object MsgPackType {
    internal object Boolean {
        operator fun invoke(value: kotlin.Boolean) = if (value) TRUE else FALSE
        const val TRUE = 0xc3.toByte()
        const val FALSE = 0xc2.toByte()
    }

    internal interface Mask<T> {
        operator fun invoke(value: T) = maskValue(value)
        fun maskValue(value: T): T
        fun test(value: T): kotlin.Boolean
        fun unMaskValue(value: T): T = maskValue(value)
    }

    internal object Int {
        const val INT8 = 0xd0.toByte()
        const val INT16 = 0xd1.toByte()
        const val INT32 = 0xd2.toByte()
        const val INT64 = 0xd3.toByte()
        const val UINT8 = 0xcc.toByte()
        const val UINT16 = 0xcd.toByte()
        const val UINT32 = 0xce.toByte()
        const val UINT64 = 0xcf.toByte()

        val POSITIVE_FIXNUM_MASK = object : Mask<Byte> {
            private val mask = 0b01111111
            override fun maskValue(value: Byte): Byte = (mask and value.toInt()).toByte()
            override fun test(value: Byte): kotlin.Boolean = (mask or value.toInt()) == mask
        }
        val NEGATIVE_FIXNUM_MASK = object : Mask<Byte> {
            private val mask = 0b11100000
            override fun maskValue(value: Byte): Byte = (mask or value.toInt()).toByte()
            override fun test(value: Byte): kotlin.Boolean = (mask and value.toInt()) == mask
            override fun unMaskValue(value: Byte): Byte = (mask xor value.toInt()).toByte()
        }
        const val MIN_NEGATIVE_SINGLE_BYTE = -32
        const val MIN_NEGATIVE_BYTE = -127
        const val MAX_UBYTE = 255
        const val MAX_USHORT = 65535
        const val MAX_UINT = 4294967295
        const val MAX_ULONG = -1 // Can't do it without unsigned types or BigInteger

        fun isByte(byte: Byte) = byte == INT8 || byte == UINT8
        fun isShort(byte: Byte) = byte == INT16 || byte == UINT16
        fun isInt(byte: Byte) = byte == INT32 || byte == UINT32
        fun isLong(byte: Byte) = byte == INT64 || byte == UINT64
    }

    internal object Float {
        const val FLOAT = 0xca.toByte()
        const val DOUBLE = 0xcb.toByte()
    }

    internal object String {
        fun isString(value: Byte): kotlin.Boolean {
            return FIXSTR_SIZE_MASK.test(value) ||
                    value == STR8 ||
                    value == STR16 ||
                    value == STR32
        }

        const val STR8 = 0xd9.toByte()
        const val STR16 = 0xda.toByte()
        const val STR32 = 0xdb.toByte()

        val FIXSTR_SIZE_MASK = object : Mask<Byte> {
            private val maskResult = 0b10100000
            private val mask = 0b11100000
            override fun maskValue(value: Byte): Byte = (maskResult or value.toInt()).toByte()
            override fun test(value: Byte): kotlin.Boolean = (mask and value.toInt()) == maskResult
            override fun unMaskValue(value: Byte): Byte = (maskResult xor value.toInt()).toByte()
        }
        const val MAX_FIXSTR_LENGTH = 31
        const val MAX_STR8_LENGTH = Int.MAX_UBYTE
        const val MAX_STR16_LENGTH = Int.MAX_USHORT
        const val MAX_STR32_LENGTH = Int.MAX_UINT
    }

    internal object Bin {
        fun isBinary(value: Byte): kotlin.Boolean {
            return value == BIN8 ||
                    value == BIN16 ||
                    value == BIN32
        }

        const val BIN8 = 0xc4.toByte()
        const val BIN16 = 0xc5.toByte()
        const val BIN32 = 0xc6.toByte()

        const val MAX_BIN8_LENGTH = Int.MAX_UBYTE
        const val MAX_BIN16_LENGTH = Int.MAX_USHORT
        const val MAX_BIN32_LENGTH = Int.MAX_UINT
    }

    internal object Array {
        fun isArray(value: Byte): kotlin.Boolean {
            return FIXARRAY_SIZE_MASK.test(value) ||
                    value == ARRAY16 ||
                    value == ARRAY32
        }

        const val ARRAY16 = 0xdc.toByte()
        const val ARRAY32 = 0xdd.toByte()

        val FIXARRAY_SIZE_MASK = object : Mask<Byte> {
            private val maskResult = 0b10010000
            private val mask = 0b11110000
            override fun maskValue(value: Byte): Byte = (maskResult or value.toInt()).toByte()
            override fun test(value: Byte): kotlin.Boolean = (mask and value.toInt()) == maskResult
            override fun unMaskValue(value: Byte): Byte = (maskResult xor value.toInt()).toByte()
        }
        const val MAX_FIXARRAY_SIZE = 15
        const val MAX_ARRAY16_LENGTH = Int.MAX_USHORT
        const val MAX_ARRAY32_LENGTH = Int.MAX_UINT
    }

    internal object Map {
        fun isMap(value: Byte): kotlin.Boolean {
            return FIXMAP_SIZE_MASK.test(value) ||
                    value == MAP16 ||
                    value == MAP32
        }

        const val MAP16 = 0xde.toByte()
        const val MAP32 = 0xdf.toByte()

        val FIXMAP_SIZE_MASK = object : Mask<Byte> {
            private val maskResult = 0b10000000
            private val mask = 0b11110000
            override fun maskValue(value: Byte): Byte = (maskResult or value.toInt()).toByte()
            override fun test(value: Byte): kotlin.Boolean = (mask and value.toInt()) == maskResult
            override fun unMaskValue(value: Byte): Byte = (maskResult xor value.toInt()).toByte()
        }
        const val MAX_FIXMAP_SIZE = 15
        const val MAX_MAP16_LENGTH = Int.MAX_USHORT
        const val MAX_MAP32_LENGTH = Int.MAX_UINT
    }

    internal object Ext {
        fun isExt(value: Byte): kotlin.Boolean {
            return TYPES.contains(value)
        }

        const val FIXEXT1 = 0xd4.toByte()
        const val FIXEXT2 = 0xd5.toByte()
        const val FIXEXT4 = 0xd6.toByte()
        const val FIXEXT8 = 0xd7.toByte()
        const val FIXEXT16 = 0xd8.toByte()
        const val EXT8 = 0xc7.toByte()
        const val EXT16 = 0xc8.toByte()
        const val EXT32 = 0xc9.toByte()

        val SIZES = hashMapOf(
            FIXEXT1 to 1,
            FIXEXT2 to 2,
            FIXEXT4 to 4,
            FIXEXT8 to 8,
            FIXEXT16 to 16
        )

        val SIZE_SIZE = hashMapOf(
            EXT8 to 1,
            EXT16 to 2,
            EXT32 to 4
        )

        val TYPES = SIZES.keys + listOf(EXT8, EXT16, EXT32)

        const val MAX_EXT8_LENGTH = Int.MAX_UBYTE
        const val MAX_EXT16_LENGTH = Int.MAX_USHORT
        const val MAX_EXT32_LENGTH = Int.MAX_UINT
    }

    const val NULL = 0xc0.toByte()
}
