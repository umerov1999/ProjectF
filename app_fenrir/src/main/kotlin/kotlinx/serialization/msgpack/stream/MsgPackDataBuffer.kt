package kotlinx.serialization.msgpack.stream

import com.google.common.math.IntMath.mod
import dev.ragnarok.fenrir.module.BufferWriteNative
import okio.BufferedSource

abstract class MsgPackDataOutputBuffer : MsgPackDataBuffer {
    abstract fun add(byte: Byte): Boolean
    abstract fun addAll(bytesList: List<Byte>): Boolean
    abstract fun addAll(bytesArray: ByteArray): Boolean
}

abstract class MsgPackDataInputBuffer : MsgPackDataBuffer {
    abstract fun skip(bytes: Int)
    abstract fun peek(): Byte
    abstract fun peekSafely(): Byte?

    // Increases index only if next byte is not null
    abstract fun nextByteOrNull(): Byte?
    abstract fun requireNextByte(): Byte
    abstract fun takeNext(next: Int): ByteArray
    abstract fun currentIndex(): Int
}

interface MsgPackDataBuffer {
    fun toByteArray(): ByteArray
}

class MsgPackDataOutputArrayBuffer : MsgPackDataOutputBuffer() {
    private val byteArrays = mutableListOf<ByteArray>()

    override fun add(byte: Byte): Boolean {
        return byteArrays.add(ByteArray(1) { byte })
    }

    override fun addAll(bytes: List<Byte>): Boolean {
        if (bytes.isNotEmpty()) {
            return byteArrays.add(bytes.toByteArray())
        }
        return true
    }

    override fun addAll(bytes: ByteArray): Boolean {
        if (bytes.isNotEmpty()) {
            return byteArrays.add(bytes)
        }
        return true
    }

    override fun toByteArray(): ByteArray {
        val totalSize = byteArrays.sumOf { it.size }
        val outputArray = ByteArray(totalSize)
        var currentIndex = 0
        byteArrays.forEach {
            it.copyInto(outputArray, currentIndex)
            currentIndex += it.size
        }
        return outputArray
    }
}

class MsgPackDataInputArrayBuffer(private val byteArray: ByteArray) : MsgPackDataInputBuffer() {
    private var index = 0
    override fun skip(bytes: Int) {
        require(bytes > 0) { "Number of bytes to take must be greater than 0!" }
        index += bytes
    }

    override fun currentIndex(): Int {
        return index
    }

    override fun peek(): Byte = byteArray.getOrNull(index) ?: throw Exception("End of stream")

    override fun peekSafely(): Byte? = byteArray.getOrNull(index)

    // Increases index only if next byte is not null
    override fun nextByteOrNull(): Byte? = byteArray.getOrNull(index)?.also { index++ }

    override fun requireNextByte(): Byte = nextByteOrNull() ?: throw Exception("End of stream")

    override fun takeNext(next: Int): ByteArray {
        require(next > 0) { "Number of bytes to take must be greater than 0!" }
        val result = ByteArray(next)
        (0 until next).forEach {
            result[it] = requireNextByte()
        }
        return result
    }

    override fun toByteArray() = byteArray
}

class MsgPackDataOutputArrayBufferCompressed : MsgPackDataOutputBuffer() {
    private val bytes = BufferWriteNative(8192)
    override fun add(byte: Byte): Boolean {
        bytes.putChar(byte)
        return true
    }

    override fun addAll(bytesList: List<Byte>): Boolean {
        if (bytesList.isNotEmpty()) {
            bytes.putByteArray(bytesList.toByteArray())
        }
        return true
    }

    override fun addAll(bytesArray: ByteArray): Boolean {
        if (bytesArray.isNotEmpty()) {
            bytes.putByteArray(bytesArray)
        }
        return true
    }

    override fun toByteArray() = bytes.compressLZ4Buffer() ?: ByteArray(0)
}

class MsgPackDataBufferedOutputArrayBuffer : MsgPackDataOutputBuffer() {
    private val bytes = ArrayList<Byte>()
    override fun add(byte: Byte): Boolean {
        if (mod(bytes.size, 8192) == 0) {
            bytes.ensureCapacity(bytes.size + 8192)
        }
        return bytes.add(byte)
    }

    override fun addAll(bytesList: List<Byte>): Boolean {
        if (bytesList.isEmpty()) {
            return true
        }
        val cap = bytes.size + bytesList.size
        val res = ((cap + 8192 - 1) / 8192) * 8192
        bytes.ensureCapacity(res)
        return bytes.addAll(bytesList)
    }

    override fun addAll(bytesArray: ByteArray) = addAll(bytesArray.toList())
    override fun toByteArray() = bytes.toByteArray()
}

class MsgPackDataInputOkio(private val bufferedSource: BufferedSource) : MsgPackDataInputBuffer() {
    private var index = 0
    private var preloaded: Byte = 0x00
    private var isPreloaded = false
    override fun skip(bytes: Int) {
        require(bytes > 0) { "Number of bytes to take must be greater than 0!" }
        bufferedSource.skip(bytes.toLong() - 1)
        index += bytes - 1
        isPreloaded = false
    }

    override fun currentIndex(): Int {
        return index
    }

    override fun peek(): Byte {
        if (!isPreloaded) {
            isPreloaded = true
            preloaded = (bufferedSource.readByte().toInt() and 0xff).toByte()
            index++
        }
        return preloaded
    }

    override fun peekSafely(): Byte? {
        return try {
            peek()
        } catch (_: Exception) {
            null
        }
    }

    // Increases index only if next byte is not null
    override fun nextByteOrNull(): Byte? = peekSafely()?.also { isPreloaded = false }
    override fun requireNextByte(): Byte = nextByteOrNull() ?: throw Exception("End of stream")
    override fun takeNext(next: Int): ByteArray {
        require(next > 0) { "Number of bytes to take must be greater than 0!" }
        val result = ByteArray(next)
        (0 until next).forEach {
            result[it] = requireNextByte()
        }
        return result
    }

    override fun toByteArray(): ByteArray {
        throw UnsupportedOperationException()
    }
}

internal fun ByteArray.toMsgPackArrayBuffer() = MsgPackDataInputArrayBuffer(this)
internal fun BufferedSource.toMsgPackBufferedSource() = MsgPackDataInputOkio(this)
//internal fun BufferedSource.toMsgPackBufferedSource() = MsgPackDataInputArrayBuffer(this.readByteArray())
