package dev.ragnarok.filegallery.util.serializeble.msgpack.utils

internal inline fun <reified T : Number> T.splitToByteArray(): ByteArray {
    val byteCount = when (T::class) {
        Byte::class -> 1
        Short::class -> 2
        Int::class -> 4
        Long::class -> 8
        else -> throw UnsupportedOperationException("Can't split number of type ${T::class} to bytes!")
    }

    val result = ByteArray(byteCount)
    (byteCount - 1).downTo(0).forEach {
        result[byteCount - (it + 1)] = ((this.toLong() shr (8 * it)) and 0xff).toByte()
    }
    return result
}

internal inline fun <reified T : Number> ByteArray.joinToNumber(): T {
    val number = mapIndexed { index, byte ->
        (byte.toLong() and 0xff) shl (8 * (size - (index + 1)))
    }.fold(0L) { acc, it ->
        acc or it
    }
    return when (T::class) {
        Byte::class -> number.toByte()
        Short::class -> number.toShort()
        Int::class -> number.toInt()
        Long::class -> number
        else -> throw UnsupportedOperationException("Can't build ${T::class} from ByteArray (${this.toList()})")
    } as T
}
