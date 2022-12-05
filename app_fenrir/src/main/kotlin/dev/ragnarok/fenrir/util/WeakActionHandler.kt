package dev.ragnarok.fenrir.util

import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.ref.WeakReference

class WeakActionHandler<T>(orig: T) : Handler(Looper.getMainLooper()) {
    private val ref: WeakReference<T> = WeakReference(orig)
    private var action: Action<T>? = null
    override fun handleMessage(msg: Message) {
        val orig = ref.get()
        if (action != null && orig != null) {
            action?.doAction(msg.what, orig)
        }
    }

    fun setAction(action: Action<T>?): WeakActionHandler<T> {
        this.action = action
        return this
    }

    interface Action<T> {
        fun doAction(what: Int, orig: T)
    }

}