package dev.ragnarok.filegallery.util

import android.content.Context
import dev.ragnarok.filegallery.R
import dev.ragnarok.filegallery.nonNullNoEmpty
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorLocalizer {
    fun localizeThrowable(context: Context, throwable: Throwable?): String {
        throwable ?: return "null"
        if (throwable is SocketTimeoutException) {
            return context.getString(R.string.error_timeout_message)
        }
        if (throwable is UnknownHostException) {
            return context.getString(R.string.error_unknown_host)
        }
        return if (throwable.message.nonNullNoEmpty()) throwable.message!! else throwable.toString()
    }
}