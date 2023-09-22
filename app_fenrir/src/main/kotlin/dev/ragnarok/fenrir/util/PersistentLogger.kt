package dev.ragnarok.fenrir.util

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.model.LogEvent
import dev.ragnarok.fenrir.settings.Settings
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
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
            .flatMapCompletable { s: String ->
                store.addLog(LogEvent.Type.ERROR, tag, s)
                    .ignoreElement()
            }
            .onErrorComplete()
            .subscribeOn(Schedulers.io())
            .subscribe({}) { }
    }

    fun logThrowableSync(tag: String, throwable: Throwable) {
        if (!Settings.get().main().isDoLogs) return
        val store = Includes.stores.tempStore()
        val cause = Utils.getCauseIfRuntime(throwable)
        getStackTrace(cause)
            .flatMapCompletable { s: String ->
                store.addLog(LogEvent.Type.ERROR, tag, s)
                    .ignoreElement()
            }
            .onErrorComplete()
            .blockingAwait()
    }

    private fun getStackTrace(throwable: Throwable): Single<String> {
        return Single.fromCallable {
            StringWriter().use { sw ->
                PrintWriter(sw).use { pw ->
                    throwable.printStackTrace(pw)
                    return@fromCallable sw.toString()
                }
            }
        }
    }
}