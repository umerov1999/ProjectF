package dev.ragnarok.fenrir.api.rest

import dev.ragnarok.fenrir.api.HttpLoggerAndParser
import dev.ragnarok.fenrir.api.model.Params
import dev.ragnarok.fenrir.api.model.response.VKResponse
import dev.ragnarok.fenrir.api.model.response.VKUrlResponse
import dev.ragnarok.fenrir.ifNonNull
import dev.ragnarok.fenrir.isJson
import dev.ragnarok.fenrir.isMsgPack
import dev.ragnarok.fenrir.kJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.decodeFromBufferedSource
import kotlinx.serialization.msgpack.MsgPack
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlin.coroutines.cancellation.CancellationException

class SimplePostHttp(
    private val baseUrl: String?,
    okHttpClient: OkHttpClient.Builder
) {
    private val client = okHttpClient.build()

    fun stop() {
        client.dispatcher.cancelAll()
    }

    fun <T : Any> requestFullUrl(
        url: String,
        body: RequestBody?,
        serial: KSerializer<T>,
        onlySuccessful: Boolean = true
    ): Flow<T> {
        return requestInternal(
            url,
            body,
            serial, onlySuccessful
        )
    }

    fun requestAndGetURLFromRedirects(
        methodOrFullUrl: String,
        body: RequestBody?
    ): Flow<VKUrlResponse> {
        return flow {
            val request = Request.Builder()
                .url(
                    if (baseUrl.isNullOrEmpty()) methodOrFullUrl else "$baseUrl/$methodOrFullUrl"
                )
            body.ifNonNull(
                { request.post(it) }, { request.get() }
            )
            val call = client.newCall(request.build())
            try {
                val response = call.execute()
                if (response.isSuccessful) {
                    val ret = VKUrlResponse()
                    ret.resultUrl = response.request.url.toString()
                    emit(ret)
                } else {
                    val ret = if (response.body.isMsgPack()) MsgPack().decodeFromOkioStream(
                        VKUrlResponse.serializer(), response.body.source()
                    ) else if (response.body.isJson()) kJson.decodeFromBufferedSource(
                        VKUrlResponse.serializer(), response.body.source()
                    ) else {
                        throw UnsupportedOperationException()
                    }
                    ret.resultUrl = null
                    emit(ret)
                }
                response.close()
            } catch (e: CancellationException) {
                call.cancel()
                throw e
            }
        }
    }

    fun <T : Any> request(
        methodOrFullUrl: String,
        body: RequestBody?,
        serial: KSerializer<T>,
        onlySuccessful: Boolean = true
    ): Flow<T> {
        return requestInternal(
            if (baseUrl.isNullOrEmpty()) methodOrFullUrl else "$baseUrl/$methodOrFullUrl",
            body,
            serial, onlySuccessful
        )
    }

    private fun <T : Any> requestInternal(
        url: String,
        body: RequestBody?,
        serial: KSerializer<T>,
        onlySuccessful: Boolean
    ): Flow<T> {
        return flow {
            val request = Request.Builder()
                .url(
                    url
                )
            body.ifNonNull(
                { request.post(it) }, { request.get() }
            )
            val call = client.newCall(request.build())
            try {
                val response = call.execute()
                if (!response.isSuccessful && onlySuccessful) {
                    throw HttpException(response.code)
                } else {
                    val ret = if (response.body.isMsgPack()) MsgPack().decodeFromOkioStream(
                        serial, response.body.source()
                    ) else kJson.decodeFromBufferedSource(
                        serial, response.body.source()
                    )
                    if (ret is VKResponse) {
                        ret.error?.let {
                            it.serializer = serial
                            val o: ArrayList<Params> = when (val stmp = response.request.body) {
                                is FormBody -> {
                                    val f = ArrayList<Params>(stmp.size)
                                    for (i in 0 until stmp.size) {
                                        val tmp = Params()
                                        tmp.key = stmp.name(i)
                                        tmp.value = stmp.value(i)
                                        f.add(tmp)
                                    }
                                    f
                                }

                                is HttpLoggerAndParser.GzipFormBody -> {
                                    val f = ArrayList<Params>(stmp.original.size)
                                    f.addAll(stmp.original)
                                    f
                                }

                                else -> {
                                    ArrayList()
                                }
                            }
                            val tmp = Params()
                            tmp.key = "post_url"
                            tmp.value = response.request.url.toString()
                            o.add(tmp)
                            it.requestParams = o
                        }
                    }
                    emit(ret)
                }
                response.close()
            } catch (e: CancellationException) {
                call.cancel()
                throw e
            }
        }
    }

    fun <T : Any> doMultipartForm(
        methodOrFullUrl: String,
        part: MultipartBody.Part,
        serial: KSerializer<T>, onlySuccessful: Boolean = true
    ): Flow<T> {
        val requestBodyMultipart: RequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM).addPart(part).build()
        return request(methodOrFullUrl, requestBodyMultipart, serial, onlySuccessful)
    }

    fun <T : Any> doMultipartFormFullUrl(
        url: String,
        part: MultipartBody.Part,
        serial: KSerializer<T>, onlySuccessful: Boolean = true
    ): Flow<T> {
        val requestBodyMultipart: RequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM).addPart(part).build()
        return requestFullUrl(url, requestBodyMultipart, serial, onlySuccessful)
    }
}
