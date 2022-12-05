package dev.ragnarok.fenrir.util.serializeble.retrofit.kotlinx.serialization

import dev.ragnarok.fenrir.api.model.response.VkResponse
import kotlinx.serialization.DeserializationStrategy
import okhttp3.ResponseBody
import retrofit2.Converter
import java.lang.reflect.Type

internal class DeserializationStrategyConverter<T>(
    private val loader: DeserializationStrategy<T>,
    private val serializer: Serializer,
    private val type: Type
) : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T {
        val result = serializer.fromResponseBody(loader, value)
        if (result is VkResponse) {
            result.error?.let {
                it.type = type
                it.serializer = serializer
            }
        }
        return result
    }
}
