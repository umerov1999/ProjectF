package dev.ragnarok.filegallery.util.serializeble.json.internal

import java.io.*
import java.nio.*
import java.nio.charset.*

internal class CharsetReader(
    private val inputStream: InputStream,
    charset: Charset
) {
    private val decoder: CharsetDecoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    private val byteBuffer: ByteBuffer = ByteBuffer.wrap(ByteArrayPool8k.take())

    // Surrogate-handling in cases when a single char is requested, but two were read
    private var hasLeftoverPotentiallySurrogateChar = false
    private var leftoverChar = 0.toChar()

    init {
        // An explicit cast is needed here due to an API change in Java 9, see #2218.
        //
        // In Java 8 and earlier, the `flip` method was final in `Buffer`, and returned a `Buffer`.
        // In Java 9 and later, the method was opened, and `ByteFuffer` overrides it, returning a `ByteBuffer`.
        //
        // You could observe this by decompiling this call with `javap`:
        // Compiled with Java 8 it produces `INVOKEVIRTUAL java/nio/ByteBuffer.flip ()Ljava/nio/Buffer;`
        // Compiled with Java 9+ it produces `INVOKEVIRTUAL java/nio/ByteBuffer.flip ()Ljava/nio/ByteBuffer;`
        //
        // This causes a `NoSuchMethodError` when running a class, compiled with a newer Java version, on Java 8.
        //
        // To mitigate that, `--bootclasspath` / `--release` options were introduced in `javac`, but there are no
        // counterparts for these options in `kotlinc`, so an explicit cast is required.
        (byteBuffer as Buffer).flip() // Make empty
    }

    @Suppress("NAME_SHADOWING")
    fun read(array: CharArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        require(offset in array.indices && length >= 0 && offset + length <= array.size) {
            "Unexpected arguments: $offset, $length, ${array.size}"
        }

        var offset = offset
        var length = length
        var bytesRead = 0
        if (hasLeftoverPotentiallySurrogateChar) {
            array[offset] = leftoverChar
            offset++
            length--
            hasLeftoverPotentiallySurrogateChar = false
            bytesRead = 1
            if (length == 0) return bytesRead
        }
        if (length == 1) {
            // Treat single-character array reads just like read()
            val c = oneShotReadSlowPath()
            if (c == -1) return if (bytesRead == 0) -1 else bytesRead
            array[offset] = c.toChar()
            return bytesRead + 1
        }
        return doRead(array, offset, length) + bytesRead
    }

    private fun doRead(array: CharArray, offset: Int, length: Int): Int {
        var charBuffer = CharBuffer.wrap(array, offset, length)
        if (charBuffer.position() != 0) {
            charBuffer = charBuffer.slice()
        }
        var isEof = false
        while (true) {
            val cr = decoder.decode(byteBuffer, charBuffer, isEof)
            if (cr.isUnderflow) {
                if (isEof) break
                if (!charBuffer.hasRemaining()) break
                val n = fillByteBuffer()
                if (n < 0) {
                    isEof = true
                    if (charBuffer.position() == 0 && !byteBuffer.hasRemaining()) break
                    decoder.reset()
                }
                continue
            }
            if (cr.isOverflow) {
                assert(charBuffer.position() > 0)
                break
            }
            cr.throwException()
        }
        if (isEof) decoder.reset()
        return if (charBuffer.position() == 0) -1
        else charBuffer.position()
    }

    private fun fillByteBuffer(): Int {
        byteBuffer.compact()
        try {
            // Read from the input stream, and then update the buffer
            val limit = byteBuffer.limit()
            val position = byteBuffer.position()
            val remaining = if (position <= limit) limit - position else 0
            val bytesRead =
                inputStream.read(byteBuffer.array(), byteBuffer.arrayOffset() + position, remaining)
            if (bytesRead < 0) return bytesRead
            byteBuffer.position(position + bytesRead)
        } finally {
            (byteBuffer as Buffer).flip() // see the `init` block in this class for the reasoning behind the cast
        }
        return byteBuffer.remaining()
    }

    private fun oneShotReadSlowPath(): Int {
        // Return the leftover char, if there is one
        if (hasLeftoverPotentiallySurrogateChar) {
            hasLeftoverPotentiallySurrogateChar = false
            return leftoverChar.code
        }

        val array = CharArray(2)
        return when (val bytesRead = read(array, 0, 2)) {
            -1 -> -1
            1 -> array[0].code
            2 -> {
                leftoverChar = array[1]
                hasLeftoverPotentiallySurrogateChar = true
                array[0].code
            }

            else -> error("Unreachable state: $bytesRead")
        }
    }

    fun release() {
        ByteArrayPool8k.release(byteBuffer.array())
    }
}
