package dev.ragnarok.fenrir.view

import dev.ragnarok.fenrir.util.Action
import java.lang.ref.WeakReference

class WeakRunnable<T>(reference: T, private val action: Action<T>) : Runnable {
    private val reference: WeakReference<T> = WeakReference(reference)
    override fun run() {
        reference.get()?.let { action.call(it) }
    }
}