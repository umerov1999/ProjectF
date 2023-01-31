package dev.ragnarok.filegallery.util

import android.content.Context
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.api.rest.HttpException
import dev.ragnarok.filegallery.nonNullNoEmpty
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorLocalizer {
    fun localizeThrowable(context: Context, throwable: Throwable?): String {
        throwable ?: return "null"
        return when (throwable) {
            is SocketTimeoutException -> {
                context.getString(R.string.error_timeout_message)
            }

            is InterruptedIOException -> {
                if ("timeout" == throwable.message || "executor rejected" == throwable.message) {
                    context.getString(R.string.error_timeout_message)
                } else {
                    throwable.message.nonNullNoEmpty({ it }, { throwable.toString() })
                }
            }

            is ConnectException, is UnknownHostException -> {
                context.getString(R.string.error_unknown_host)
            }

            is HttpException -> {
                if (throwable.code < 0) {
                    context.getString(R.string.client_rest_shutdown)
                } else {
                    context.getString(R.string.internal_server_error, throwable.code)
                }
            }

            else -> if (throwable.message.nonNullNoEmpty()) throwable.message!! else throwable.toString()
        }
    }
}