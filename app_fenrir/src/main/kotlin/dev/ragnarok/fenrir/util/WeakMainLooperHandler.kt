package dev.ragnarok.fenrir.util

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

abstract class WeakMainLooperHandler<T>(t: T) : Handler(Looper.getMainLooper()) {

    private val reference: WeakReference<T> = WeakReference(t)

    final override fun handleMessage(msg: Message) {
        reference.get()?.run {
            handleMessage(this, msg)
        }
    }

    open fun handleMessage(t: T, msg: Message) {

    }
}