package dev.ragnarok.filegallery.util.serializeble.retrofit.kotlinx.serialization

import dev.ragnarok.filegallery.isMsgPack
import dev.ragnarok.filegallery.util.serializeble.json.Json
import dev.ragnarok.filegallery.util.serializeble.json.internal.JavaStreamSerialReader
import dev.ragnarok.filegallery.util.serializeble.json.internal.decodeByReader
import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPack
import kotlinx.serialization.*
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.lang.reflect.Type

sealed class Serializer {
    abstract fun <T> fromResponseBody(loader: DeserializationStrategy<T>, body: ResponseBody): T
    abstract fun <T> toRequestBody(
        contentType: MediaType,
        saver: SerializationStrategy<T>,
        value: T
    ): RequestBody

    protected abstract val format: SerialFormat

    @ExperimentalSerializationApi // serializer(Type) is not stable.
    fun serializer(type: Type): KSerializer<Any> = format.serializersModule.serializer(type)

    class FromString(override val format: StringFormat) : Serializer() {
        override fun <T> fromResponseBody(
            loader: DeserializationStrategy<T>,
            body: ResponseBody
        ): T {
            val string = body.string()
            return format.decodeFromString(loader, string)
        }

        override fun <T> toRequestBody(
            contentType: MediaType,
            saver: SerializationStrategy<T>,
            value: T
        ): RequestBody {
            val string = format.encodeToString(saver, value)
            return string.toRequestBody(contentType)
        }
    }

    class FromJsonMsgPack(override val format: Json, private val msgPack: MsgPack) : Serializer() {
        override fun <T> fromResponseBody(
            loader: DeserializationStrategy<T>,
            body: ResponseBody
        ): T {
            if (body.isMsgPack()) {
                return msgPack.decodeFromOkioStream(loader, body.source())
            }
            return format.decodeByReader(loader, JavaStreamSerialReader(body.byteStream()))
        }

        override fun <T> toRequestBody(
            contentType: MediaType,
            saver: SerializationStrategy<T>,
            value: T
        ): RequestBody {
            val string = format.encodeToString(saver, value)
            return string.toRequestBody(contentType)
        }
    }

    class FromJson(override val format: Json) : Serializer() {
        override fun <T> fromResponseBody(
            loader: DeserializationStrategy<T>,
            body: ResponseBody
        ): T {
            return format.decodeByReader(loader, JavaStreamSerialReader(body.byteStream()))
        }

        override fun <T> toRequestBody(
            contentType: MediaType,
            saver: SerializationStrategy<T>,
            value: T
        ): RequestBody {
            val string = format.encodeToString(saver, value)
            return string.toRequestBody(contentType)
        }
    }

    class FromMsgPack(override val format: MsgPack) : Serializer() {
        override fun <T> fromResponseBody(
            loader: DeserializationStrategy<T>,
            body: ResponseBody
        ): T {
            return format.decodeFromOkioStream(loader, body.source())
        }

        override fun <T> toRequestBody(
            contentType: MediaType,
            saver: SerializationStrategy<T>,
            value: T
        ): RequestBody {
            val bytes = format.encodeToByteArray(saver, value)
            return bytes.toRequestBody(contentType, 0, bytes.size)
        }
    }

    class FromBytes(override val format: BinaryFormat) : Serializer() {
        override fun <T> fromResponseBody(
            loader: DeserializationStrategy<T>,
            body: ResponseBody
        ): T {
            val bytes = body.bytes()
            return format.decodeFromByteArray(loader, bytes)
        }

        override fun <T> toRequestBody(
            contentType: MediaType,
            saver: SerializationStrategy<T>,
            value: T
        ): RequestBody {
            val bytes = format.encodeToByteArray(saver, value)
            return bytes.toRequestBody(contentType, 0, bytes.size)
        }
    }
}
