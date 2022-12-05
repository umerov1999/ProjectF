package dev.ragnarok.fenrir.util

import io.reactivex.rxjava3.functions.Consumer
import java.lang.ref.WeakReference

class WeakConsumer<T : Any>(orig: Consumer<T>) : Consumer<T> {
    private val ref: WeakReference<Consumer<T>> = WeakReference(orig)
    override fun accept(t: T) {
        ref.get()?.accept(t)
    }
}