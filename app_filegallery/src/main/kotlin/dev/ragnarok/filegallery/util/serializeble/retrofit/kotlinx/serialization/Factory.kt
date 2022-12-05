@file:JvmName("KotlinSerializationConverterFactory")

package dev.ragnarok.filegallery.util.serializeble.retrofit.kotlinx.serialization

import dev.ragnarok.filegallery.util.serializeble.json.Json
import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPack
import dev.ragnarok.filegallery.util.serializeble.retrofit.kotlinx.serialization.Serializer.FromBytes
import dev.ragnarok.filegallery.util.serializeble.retrofit.kotlinx.serialization.Serializer.FromString
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.StringFormat
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

@ExperimentalSerializationApi
internal class Factory(
    private val contentType: MediaType,
    private val serializer: Serializer
) : Converter.Factory() {
    @Suppress("RedundantNullableReturnType") // Retaining interface contract.
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val loader = serializer.serializer(type)
        return DeserializationStrategyConverter(loader, serializer)
    }

    @Suppress("RedundantNullableReturnType") // Retaining interface contract.
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        val saver = serializer.serializer(type)
        return SerializationStrategyConverter(contentType, saver, serializer)
    }
}

/**
 * Return a [Converter.Factory] which uses Kotlin serialization for string-based payloads.
 *
 * Because Kotlin serialization is so flexible in the types it supports, this converter assumes
 * that it can handle all types. If you are mixing this with something else, you must add this
 * instance last to allow the other converters a chance to see their types.
 */
@ExperimentalSerializationApi
@JvmName("create")
fun StringFormat.asConverterFactory(contentType: MediaType): Converter.Factory {
    return Factory(contentType, FromString(this))
}

@ExperimentalSerializationApi
@JvmName("create")
fun StringFormat.asConverterFactory(): Converter.Factory {
    return Factory("application/json; charset=UTF-8".toMediaType(), FromString(this))
}

@ExperimentalSerializationApi
@JvmName("create")
fun Json.asConverterFactory(): Converter.Factory {
    return Factory("application/json; charset=UTF-8".toMediaType(), Serializer.FromJson(this))
}

@ExperimentalSerializationApi
fun jsonMsgPackConverterFactory(json: Json, msgPack: MsgPack): Converter.Factory {
    return Factory(
        "application/json; charset=UTF-8".toMediaType(),
        Serializer.FromJsonMsgPack(json, msgPack)
    )
}

@ExperimentalSerializationApi
@JvmName("create")
fun MsgPack.asConverterFactory(): Converter.Factory {
    return Factory(
        "application/x-msgpack; charset=utf-8".toMediaType(),
        Serializer.FromMsgPack(this)
    )
}

/**
 * Return a [Converter.Factory] which uses Kotlin serialization for byte-based payloads.
 *
 * Because Kotlin serialization is so flexible in the types it supports, this converter assumes
 * that it can handle all types. If you are mixing this with something else, you must add this
 * instance last to allow the other converters a chance to see their types.
 */
@ExperimentalSerializationApi
@JvmName("create")
fun BinaryFormat.asConverterFactory(contentType: MediaType): Converter.Factory {
    return Factory(contentType, FromBytes(this))
}
