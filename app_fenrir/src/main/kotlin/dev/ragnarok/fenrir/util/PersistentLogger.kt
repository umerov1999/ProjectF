package dev.ragnarok.fenrir.util

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.model.LogEvent
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.hiddenIO
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.ignoreElement
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.syncSingleSafe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import java.io.PrintWriter
import java.io.StringWriter

object PersistentLogger {
    @SuppressLint("CheckResult")
    fun logThrowable(tag: String, throwable: Throwable) {
        if (!Settings.get().main().isDoLogs) return
        val store = Includes.stores.tempStore()
        val cause = Utils.getCauseIfRuntime(throwable)
        cause.printStackTrace()
        getStackTrace(cause)
            .flatMapConcat { s ->
                store.addLog(LogEvent.Type.ERROR, tag, s)
                    .ignoreElement()
            }.hiddenIO()
    }

    fun logThrowableSync(tag: String, throwable: Throwable) {
        if (!Settings.get().main().isDoLogs) return
        val store = Includes.stores.tempStore()
        val cause = Utils.getCauseIfRuntime(throwable)
        getStackTrace(cause)
            .flatMapConcat { s ->
                store.addLog(LogEvent.Type.ERROR, tag, s)
                    .ignoreElement()
            }
            .syncSingleSafe()
    }

    private fun getStackTrace(throwable: Throwable): Flow<String> {
        return flow {
            StringWriter().use { sw ->
                PrintWriter(sw).use { pw ->
                    throwable.printStackTrace(pw)
                    emit(sw.toString())
                }
            }
        }
    }
}