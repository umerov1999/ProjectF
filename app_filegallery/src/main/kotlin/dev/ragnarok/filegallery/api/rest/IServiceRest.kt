package dev.ragnarok.filegallery.api.rest

import dev.ragnarok.filegallery.api.model.Items
import dev.ragnarok.filegallery.api.model.response.BaseResponse
import dev.ragnarok.filegallery.kJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.ref.WeakReference

abstract class IServiceRest {
    private var restClient: WeakReference<SimplePostHttp>? = null

    val rest: SimplePostHttp
        get() = restClient?.get() ?: throw HttpException(-1)

    fun addon(client: SimplePostHttp?) {
        restClient = WeakReference(client)
    }

    companion object {
        val baseInt: KSerializer<BaseResponse<Int>>
            get() = BaseResponse.serializer(Int.serializer())

        val baseLong: KSerializer<BaseResponse<Long>>
            get() = BaseResponse.serializer(Long.serializer())

        val baseString: KSerializer<BaseResponse<String>>
            get() = BaseResponse.serializer(String.serializer())

        inline fun <reified T : Any> base(serial: KSerializer<T>): KSerializer<BaseResponse<T>> {
            return BaseResponse.serializer(serial)
        }

        inline fun <reified T : Any> baseList(serial: KSerializer<T>): KSerializer<BaseResponse<List<T>>> {
            return BaseResponse.serializer(ListSerializer(serial))
        }

        inline fun <reified T : Any> items(serial: KSerializer<T>): KSerializer<BaseResponse<Items<T>>> {
            return BaseResponse.serializer(Items.serializer(serial))
        }

        private fun toSerialStr(obj: Any?): String? {
            return when (obj) {
                is String -> {
                    obj
                }
                is Byte, is Short, is Int, is Long, is Float, is Double -> {
                    obj.toString()
                }
                is Boolean -> {
                    if (obj) "1" else "0"
                }
                else -> null
            }
        }

        inline fun <reified T : Any> jsonForm(obj: T, serial: KSerializer<T>): RequestBody {
            return kJson.encodeToString(serial, obj)
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        }

        fun form(vararg pairs: Pair<String, Any?>): FormBody {
            val formBuilder = FormBody.Builder()
            for ((first, second) in pairs) {
                toSerialStr(second)?.let {
                    formBuilder.add(first, it)
                }
            }
            return formBuilder.build()
        }
    }
}
