package dev.ragnarok.filegallery.util.serializeble.msgpack

import dev.ragnarok.filegallery.util.serializeble.json.*
import dev.ragnarok.filegallery.util.serializeble.msgpack.exceptions.MsgPackSerializationException
import dev.ragnarok.filegallery.util.serializeble.msgpack.internal.BasicMsgPackDecoder
import dev.ragnarok.filegallery.util.serializeble.msgpack.types.MsgPackType
import dev.ragnarok.filegallery.util.serializeble.msgpack.utils.joinToNumber

internal class MsgPackTreeReader(
    private val basicMsgPackDecoder: BasicMsgPackDecoder
) {
    private var stackDepth = 0
    private fun readObject(): JsonElement = readObjectImpl {
        read()
    }

    private suspend fun DeepRecursiveScope<Unit, JsonElement>.readObject(): JsonElement =
        readObjectImpl { callRecursive(Unit) }

    private fun getArraySize(): Int {
        val type = basicMsgPackDecoder.dataBuffer.requireNextByte()
        return when {
            MsgPackType.Array.FIXARRAY_SIZE_MASK.test(type) -> MsgPackType.Array.FIXARRAY_SIZE_MASK.unMaskValue(
                type
            ).toInt()

            type == MsgPackType.Array.ARRAY16 -> basicMsgPackDecoder.dataBuffer.takeNext(2)
                .joinToNumber()
            type == MsgPackType.Array.ARRAY32 -> {
                if (basicMsgPackDecoder.configuration.preventOverflows) {
                    val number = basicMsgPackDecoder.dataBuffer.takeNext(4).joinToNumber<Long>()
                    if (number !in Int.MIN_VALUE..Int.MAX_VALUE) {
                        throw MsgPackSerializationException.overflowError(basicMsgPackDecoder.dataBuffer)
                    } else {
                        number.toInt()
                    }
                } else {
                    basicMsgPackDecoder.dataBuffer.takeNext(4).joinToNumber()
                }
            }

            else -> {
                throw MsgPackSerializationException.deserialization(
                    basicMsgPackDecoder.dataBuffer,
                    "Unknown array type: $type"
                )
            }
        }
    }

    private fun getMapSize(): Int {
        val type = basicMsgPackDecoder.dataBuffer.requireNextByte()
        return when {
            MsgPackType.Map.FIXMAP_SIZE_MASK.test(type) -> MsgPackType.Map.FIXMAP_SIZE_MASK.unMaskValue(
                type
            ).toInt()

            type == MsgPackType.Map.MAP16 -> basicMsgPackDecoder.dataBuffer.takeNext(2)
                .joinToNumber()
            type == MsgPackType.Map.MAP32 -> {
                if (basicMsgPackDecoder.configuration.preventOverflows) {
                    val number = basicMsgPackDecoder.dataBuffer.takeNext(4).joinToNumber<Long>()
                    if (number !in Int.MIN_VALUE..Int.MAX_VALUE) {
                        throw MsgPackSerializationException.overflowError(basicMsgPackDecoder.dataBuffer)
                    } else {
                        number.toInt()
                    }
                } else {
                    basicMsgPackDecoder.dataBuffer.takeNext(4).joinToNumber()
                }
            }

            else -> {
                throw MsgPackSerializationException.deserialization(
                    basicMsgPackDecoder.dataBuffer,
                    "Unknown map type: $type"
                )
            }
        }
    }

    private inline fun readObjectImpl(reader: () -> JsonElement): JsonObject {
        val sz = getMapSize()
        val result = linkedMapOf<String, JsonElement>()
        for (i in 0 until sz) {
            val keyJ = basicMsgPackDecoder.dataBuffer.peek()
            if (!MsgPackType.String.isString(keyJ)) {
                throw Exception("Key must be string")
            }
            result[basicMsgPackDecoder.decodeString()] = reader()
        }
        return JsonObject(result)
    }

    private fun readArray(): JsonElement {
        val sz = getArraySize()
        val result = ArrayList<JsonElement>(sz)
        for (i in 0 until sz) {
            result.add(read())
        }
        return JsonArray(result)
    }

    fun read(): JsonElement {
        val token = basicMsgPackDecoder.dataBuffer.peek()
        return when {
            MsgPackType.Int.POSITIVE_FIXNUM_MASK.test(token) or MsgPackType.Int.NEGATIVE_FIXNUM_MASK.test(
                token
            ) || MsgPackType.Int.INT8 == token -> JsonPrimitive(basicMsgPackDecoder.decodeByte())

            MsgPackType.Int.UINT8 == token || MsgPackType.Int.INT16 == token -> JsonPrimitive(
                basicMsgPackDecoder.decodeShort()
            )
            MsgPackType.Int.UINT16 == token || MsgPackType.Int.INT32 == token -> JsonPrimitive(
                basicMsgPackDecoder.decodeInt()
            )
            MsgPackType.Int.UINT32 == token || MsgPackType.Int.INT64 == token || MsgPackType.Int.UINT64 == token -> JsonPrimitive(
                basicMsgPackDecoder.decodeLong()
            )
            token == MsgPackType.Float.FLOAT -> JsonPrimitive(basicMsgPackDecoder.decodeFloat())
            token == MsgPackType.Float.DOUBLE -> JsonPrimitive(basicMsgPackDecoder.decodeDouble())
            MsgPackType.String.isString(token) -> JsonPrimitive(
                basicMsgPackDecoder.decodeString()
            )

            MsgPackType.Bin.isBinary(token) -> {
                val pp: ByteArray = basicMsgPackDecoder.decodeByteArray()
                val list = ArrayList<JsonPrimitive>(pp.size)
                for (i in pp) {
                    list.add(JsonPrimitive(i))
                }
                JsonArray(list)
            }

            token == MsgPackType.Boolean.FALSE -> {
                basicMsgPackDecoder.dataBuffer.skip(1)
                JsonPrimitive(false)
            }

            token == MsgPackType.Boolean.TRUE -> {
                basicMsgPackDecoder.dataBuffer.skip(1)
                JsonPrimitive(
                    true
                )
            }

            token == MsgPackType.NULL -> {
                basicMsgPackDecoder.dataBuffer.skip(1)
                JsonNull
            }

            MsgPackType.Map.isMap(token) -> {
                val result = if (++stackDepth == 200) {
                    readDeepRecursive()
                } else {
                    readObject()
                }
                --stackDepth
                result
            }

            MsgPackType.Array.isArray(token) -> readArray()
            else -> throw Exception("Can't begin reading element, unexpected token $token")
        }
    }

    private fun readDeepRecursive(): JsonElement = DeepRecursiveFunction {
        val token = basicMsgPackDecoder.dataBuffer.peek()
        when {
            MsgPackType.Int.POSITIVE_FIXNUM_MASK.test(token) or MsgPackType.Int.NEGATIVE_FIXNUM_MASK.test(
                token
            ) || MsgPackType.Int.INT8 == token -> JsonPrimitive(basicMsgPackDecoder.decodeByte())

            MsgPackType.Int.UINT8 == token || MsgPackType.Int.INT16 == token -> JsonPrimitive(
                basicMsgPackDecoder.decodeShort()
            )
            MsgPackType.Int.UINT16 == token || MsgPackType.Int.INT32 == token -> JsonPrimitive(
                basicMsgPackDecoder.decodeInt()
            )
            MsgPackType.Int.UINT32 == token || MsgPackType.Int.INT64 == token || MsgPackType.Int.UINT64 == token -> JsonPrimitive(
                basicMsgPackDecoder.decodeLong()
            )
            token == MsgPackType.Float.FLOAT -> JsonPrimitive(basicMsgPackDecoder.decodeFloat())
            token == MsgPackType.Float.DOUBLE -> JsonPrimitive(basicMsgPackDecoder.decodeDouble())
            MsgPackType.String.isString(token) -> JsonPrimitive(
                basicMsgPackDecoder.decodeString()
            )

            MsgPackType.Bin.isBinary(token) -> {
                val pp: ByteArray = basicMsgPackDecoder.decodeByteArray()
                val list = ArrayList<JsonPrimitive>(pp.size)
                for (i in pp) {
                    list.add(JsonPrimitive(i))
                }
                JsonArray(list)
            }

            token == MsgPackType.Boolean.FALSE -> {
                basicMsgPackDecoder.dataBuffer.skip(1)
                JsonPrimitive(false)
            }

            token == MsgPackType.Boolean.TRUE -> {
                basicMsgPackDecoder.dataBuffer.skip(1)
                JsonPrimitive(
                    true
                )
            }

            token == MsgPackType.NULL -> {
                basicMsgPackDecoder.dataBuffer.skip(1)
                JsonNull
            }

            MsgPackType.Map.isMap(token) -> readObject()
            MsgPackType.Array.isArray(token) -> readArray()
            else -> throw Exception("Can't begin reading element, unexpected token $token")
        }
    }.invoke(Unit)
}
