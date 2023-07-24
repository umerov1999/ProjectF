package dev.ragnarok.filegallery.api.rest

import dev.ragnarok.filegallery.ifNonNull
import dev.ragnarok.filegallery.isMsgPack
import dev.ragnarok.filegallery.kJson
import dev.ragnarok.filegallery.util.serializeble.json.decodeFromBufferedSource
import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPack
import io.reactivex.rxjava3.core.Single
import kotlinx.serialization.KSerializer
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

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
    ): Single<T> {
        return requestInternal(
            url,
            body,
            serial, onlySuccessful
        )
    }

    fun <T : Any> request(
        methodOrFullUrl: String,
        body: RequestBody?,
        serial: KSerializer<T>,
        onlySuccessful: Boolean = true
    ): Single<T> {
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
    ): Single<T> {
        return Single.create { emitter ->
            val request = Request.Builder()
                .url(
                    url
                )
            body.ifNonNull(
                { request.post(it) }, { request.get() }
            )
            val call = client.newCall(request.build())
            emitter.setCancellable { call.cancel() }
            try {
                val response = call.execute()
                if (!response.isSuccessful && onlySuccessful) {
                    emitter.tryOnError(HttpException(response.code))
                } else {
                    val ret = if (response.body.isMsgPack()) MsgPack().decodeFromOkioStream(
                        serial, response.body.source()
                    ) else kJson.decodeFromBufferedSource(
                        serial, response.body.source()
                    )
                    emitter.onSuccess(
                        ret
                    )
                }
                response.close()
            } catch (e: Exception) {
                emitter.tryOnError(e)
            }
        }
    }

    fun <T : Any> doMultipartForm(
        methodOrFullUrl: String,
        part: MultipartBody.Part,
        serial: KSerializer<T>, onlySuccessful: Boolean = true
    ): Single<T> {
        val requestBodyMultipart: RequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM).addPart(part).build()
        return request(methodOrFullUrl, requestBodyMultipart, serial, onlySuccessful)
    }

    fun <T : Any> doMultipartFormFullUrl(
        url: String,
        part: MultipartBody.Part,
        serial: KSerializer<T>, onlySuccessful: Boolean = true
    ): Single<T> {
        val requestBodyMultipart: RequestBody =
            MultipartBody.Builder().setType(MultipartBody.FORM).addPart(part).build()
        return requestFullUrl(url, requestBodyMultipart, serial, onlySuccessful)
    }
}
