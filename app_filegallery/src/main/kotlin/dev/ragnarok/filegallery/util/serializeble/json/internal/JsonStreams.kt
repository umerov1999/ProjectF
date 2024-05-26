package dev.ragnarok.filegallery.util.serializeble.json.internal

import dev.ragnarok.filegallery.util.serializeble.json.DecodeSequenceMode
import dev.ragnarok.filegallery.util.serializeble.json.Json
import dev.ragnarok.filegallery.util.serializeble.json.internal.lexer.BATCH_SIZE
import dev.ragnarok.filegallery.util.serializeble.json.internal.lexer.ReaderJsonLexer
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

internal annotation class JsonFriendModuleApi

@JsonFriendModuleApi
interface InternalJsonWriter {
    fun writeLong(value: Long)
    fun writeChar(char: Char)
    fun write(text: String)
    fun writeQuoted(text: String)
    fun release()
}

@JsonFriendModuleApi
interface InternalJsonReader {
    fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int
}

@JsonFriendModuleApi
fun <T> encodeByWriter(
    json: Json,
    writer: InternalJsonWriter,
    serializer: SerializationStrategy<T>,
    value: T
) {
    val encoder = StreamingJsonEncoder(
        writer, json,
        WriteMode.OBJ,
        arrayOfNulls(WriteMode.entries.size)
    )
    encoder.encodeSerializableValue(serializer, value)
}

@JsonFriendModuleApi
fun <T> decodeByReader(
    json: Json,
    deserializer: DeserializationStrategy<T>,
    reader: InternalJsonReader
): T {
    val lexer = ReaderJsonLexer(json, reader)
    try {
        val input = StreamingJsonDecoder(json, WriteMode.OBJ, lexer, deserializer.descriptor, null)
        val result = input.decodeSerializableValue(deserializer)
        lexer.expectEof()
        return result
    } finally {
        lexer.release()
    }
}

@JsonFriendModuleApi
@ExperimentalSerializationApi
fun <T> decodeToSequenceByReader(
    json: Json,
    reader: InternalJsonReader,
    deserializer: DeserializationStrategy<T>,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> {
    val lexer = ReaderJsonLexer(
        json,
        reader,
        CharArray(BATCH_SIZE)
    ) // Unpooled buffer due to lazy nature of sequence
    val iter = JsonIterator(format, json, lexer, deserializer)
    return Sequence { iter }.constrainOnce()
}

@JsonFriendModuleApi
@ExperimentalSerializationApi
inline fun <reified T> decodeToSequenceByReader(
    json: Json,
    reader: InternalJsonReader,
    format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT
): Sequence<T> = decodeToSequenceByReader(json, reader, json.serializersModule.serializer(), format)
