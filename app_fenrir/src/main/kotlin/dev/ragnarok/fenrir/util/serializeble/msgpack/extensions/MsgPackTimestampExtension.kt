package dev.ragnarok.fenrir.util.serializeble.msgpack.extensions

import dev.ragnarok.fenrir.util.serializeble.msgpack.exceptions.MsgPackSerializationException
import dev.ragnarok.fenrir.util.serializeble.msgpack.utils.joinToNumber
import dev.ragnarok.fenrir.util.serializeble.msgpack.utils.splitToByteArray
import kotlinx.serialization.Serializable

@Serializable(with = MsgPackTimestampExtensionSerializer::class)
sealed class MsgPackTimestamp {
    companion object {
        private const val NANOSECONDS_MAX = 999999999
    }

    data class T32(val seconds: Long) : MsgPackTimestamp()
    data class T64(val seconds: Long, val nanoseconds: Int = 0) : MsgPackTimestamp() {
        init {
            if (nanoseconds > NANOSECONDS_MAX) {
                throw IllegalArgumentException("Nanoseconds part may not be larger than $NANOSECONDS_MAX. Found: $nanoseconds")
            }
        }
    }

    data class T92(val seconds: Long, val nanoseconds: Long = 0) : MsgPackTimestamp() {
        init {
            if (nanoseconds > NANOSECONDS_MAX) {
                throw IllegalArgumentException("Nanoseconds part may not be larger than $NANOSECONDS_MAX. Found: $nanoseconds")
            }
        }
    }
}

open class MsgPackTimestampExtensionSerializer :
    BaseMsgPackExtensionSerializer<MsgPackTimestamp>() {
    companion object Default : MsgPackTimestampExtensionSerializer() {
        private const val TIMESTAMP_96_DATA_SIZE = 12
    }

    final override val extTypeId: Byte = -1

    final override fun deserialize(extension: MsgPackExtension): MsgPackTimestamp {
        when (extension.type) {
            MsgPackExtension.Type.FIXEXT4 -> {
                // Working with Timestamp 32
                val timestamp = extension.data.joinToNumber<Long>()
                return MsgPackTimestamp.T32(timestamp)
            }
            MsgPackExtension.Type.FIXEXT8 -> {
                // Working with Timestamp 64
                val nanoseconds = extension.data
                    .take(4)
                    .toByteArray()
                    .joinToNumber<Long>()
                    .shr(2) // Shift right by 2 to get 30-bit number
                    .toInt()
                val seconds = extension.data[3].toLong().shl(62).ushr(30) +
                        extension.data
                            .takeLast(4)
                            .toByteArray()
                            .joinToNumber<Long>()
                return MsgPackTimestamp.T64(seconds, nanoseconds)
            }
            MsgPackExtension.Type.EXT8 -> {
                // Working with Timestamp 96
                if (extension.data.size != TIMESTAMP_96_DATA_SIZE) {
                    throw MsgPackSerializationException.genericExtensionError(
                        extension,
                        "Error when parsing datetime. Expected data size of $TIMESTAMP_96_DATA_SIZE, but found ${extension.data.size}"
                    )
                }
                val nanoseconds = extension.data
                    .take(4)
                    .toByteArray()
                    .joinToNumber<Long>()
                val seconds = extension.data
                    .takeLast(8)
                    .toByteArray()
                    .joinToNumber<Long>()
                return MsgPackTimestamp.T92(seconds, nanoseconds)
            }
            else -> throw MsgPackSerializationException.genericExtensionError(
                extension,
                "Unsupported extension type for timestamp: ${extension.type}"
            )
        }
    }

    final override fun serialize(extension: MsgPackTimestamp): MsgPackExtension {
        return when (extension) {
            is MsgPackTimestamp.T32 -> MsgPackExtension(
                MsgPackExtension.Type.FIXEXT4,
                extTypeId,
                extension.seconds.toInt().splitToByteArray()
            )
            is MsgPackTimestamp.T64 -> {
                val nanoseconds = extension.nanoseconds.shl(2).splitToByteArray()
                val nanoLastByte = nanoseconds.last().toInt() and 0xff
                val seconds = extension.seconds.splitToByteArray().takeLast(5)
                val secondsFirstByte = seconds.first().toInt() and 0xff
                val combinedByte: Byte =
                    ((nanoLastByte or (secondsFirstByte ushr 6)) and 0xff).toByte()
                MsgPackExtension(
                    MsgPackExtension.Type.FIXEXT8,
                    extTypeId,
                    nanoseconds.take(3).toByteArray() + combinedByte + seconds.takeLast(4)
                )
            }
            is MsgPackTimestamp.T92 -> MsgPackExtension(
                MsgPackExtension.Type.EXT8,
                extTypeId,
                extension.nanoseconds.toInt()
                    .splitToByteArray() + extension.seconds.splitToByteArray()
            )
        }
    }
}
