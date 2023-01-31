package dev.ragnarok.fenrir.util

import com.github.luben.zstd.ZstdInputStream
import dev.ragnarok.fenrir.module.FenrirNative
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.internal.http.promisesBody
import okio.GzipSource
import okio.buffer
import okio.source

object UncompressDefaultInterceptor : Interceptor {
    private fun uncompress(response: Response): Response {
        if (!response.promisesBody()) {
            return response
        }
        val body = response.body
        val encoding = response.header("Content-Encoding") ?: return response

        val decompressedSource = when {
            encoding.equals("zstd", ignoreCase = true) ->
                ZstdInputStream(body.source().inputStream()).source().buffer()

            encoding.equals("gzip", ignoreCase = true) ->
                GzipSource(body.source()).buffer()

            else -> return response
        }

        return response.newBuilder()
            .addHeader("Compressed-Content-Length", response.header("Content-Length") ?: "-1")
            .removeHeader("Content-Encoding")
            .removeHeader("Content-Length")
            .body(decompressedSource.asResponseBody(body.contentType(), -1))
            .build()
    }

    override fun intercept(chain: Interceptor.Chain): Response =
        if (!Utils.isCompressIncomingTraffic) {
            val request = chain.request().newBuilder()
                .header("Accept-Encoding", "none")
                .build()

            chain.proceed(request)
        } else {
            if (FenrirNative.isNativeLoaded) {
                val request = chain.request().newBuilder()
                    .header("Accept-Encoding", "zstd")
                    .build()
                uncompress(chain.proceed(request))
            } else {
                chain.proceed(chain.request())
            }
        }
}
