package dev.ragnarok.fenrir.module

import java.io.InputStream

class BufferWriteNative(reserved: Int) {
    private external fun allocateBuffer(reserved: Int): Long
    private external fun putByteArray(pointer: Long, value: ByteArray, size: Int)
    private external fun putChar(pointer: Long, value: Byte)
    private external fun compressLZ4Buffer(pointer: Long): ByteArray?
    private external fun deCompressLZ4Buffer(pointer: Long): ByteArray?
    private external fun releaseBuffer(pointer: Long)
    private external fun endString(pointer: Long)
    private external fun bufferSize(pointer: Long): Int

    var pointer: Long = 0
        private set

    init {
        pointer = allocateBuffer(reserved)
    }

    fun compressLZ4Buffer(): ByteArray? {
        if (pointer == 0L) {
            return null
        }
        return compressLZ4Buffer(pointer)
    }

    fun deCompressLZ4Buffer(): ByteArray? {
        if (pointer == 0L) {
            return null
        }
        return deCompressLZ4Buffer(pointer)
    }

    fun putByteArray(value: ByteArray, size: Int) {
        if (pointer == 0L) {
            return
        }
        putByteArray(pointer, value, size)
    }

    fun putByteArray(value: ByteArray) {
        if (pointer == 0L) {
            return
        }
        putByteArray(pointer, value, value.size)
    }

    fun putChar(value: Byte) {
        if (pointer == 0L) {
            return
        }
        putChar(pointer, value)
    }

    fun endString() {
        if (pointer == 0L) {
            return
        }
        endString(pointer)
    }

    fun putStream(inputStream: InputStream) {
        if (pointer == 0L) {
            return
        }
        try {
            var readLen: Int
            val buffer = ByteArray(8096)
            while (inputStream.read(buffer, 0, buffer.size).also { readLen = it } >= 0) {
                putByteArray(pointer, buffer, readLen)
            }
        } catch (e: Throwable) {
            return
        }
    }

    fun bufferSize(): Int {
        if (pointer == 0L) {
            return 0
        }
        return bufferSize(pointer)
    }

    fun release() {
        if (pointer == 0L) {
            return
        }
        releaseBuffer(pointer)
        pointer = 0
    }

    protected fun finalize() {
        if (pointer == 0L) {
            return
        }
        releaseBuffer(pointer)
        pointer = 0
    }

    companion object {
        fun fromStream(inputStream: InputStream): BufferWriteNative {
            val ret = BufferWriteNative(inputStream.available())
            ret.putStream(inputStream)
            return ret
        }

        fun fromStreamEndlessNull(inputStream: InputStream): BufferWriteNative {
            val ret = BufferWriteNative(inputStream.available() + 1)
            ret.putStream(inputStream)
            ret.endString()
            return ret
        }
    }
}