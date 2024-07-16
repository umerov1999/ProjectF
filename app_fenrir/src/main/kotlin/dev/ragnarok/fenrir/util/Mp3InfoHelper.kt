package dev.ragnarok.fenrir.util

import android.content.Context
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Request
import kotlin.coroutines.cancellation.CancellationException

object Mp3InfoHelper {
    fun getLength(url: String): Flow<Long> {
        return flow {
            val builder = Utils.createOkHttp(Constants.API_TIMEOUT, false)
            val request: Request = Request.Builder()
                .url(url)
                .build()
            val call = builder.build().newCall(request)
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    throw Exception(
                        "Server return " + response.code +
                                " " + response.message
                    )
                } else {
                    val length = response.header("Content-Length")
                    response.body.close()
                    response.close()
                    if (length.isNullOrEmpty()) {
                        throw Exception("Empty content length!")
                    }
                    emit(length.toLong())
                }
            } catch (e: CancellationException) {
                call.cancel()
                throw e
            }
        }
    }

    fun getBitrate(duration: Int, size: Long): Int {
        return ((((size / duration) * 8)) / 1000).toInt()
    }

    fun getBitrate(context: Context, duration: Int, size: Long): String {
        return context.getString(
            R.string.bitrate,
            ((((size / duration) * 8)) / 1000).toInt(),
            Utils.BytesToSize(size)
        )
    }
}