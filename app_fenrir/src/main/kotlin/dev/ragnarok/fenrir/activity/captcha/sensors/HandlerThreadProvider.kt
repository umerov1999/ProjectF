package dev.ragnarok.fenrir.activity.captcha.sensors

import android.os.HandlerThread
import java.util.concurrent.atomic.AtomicInteger

internal class HandlerThreadProvider(val name: String) {

    @Volatile
    private var handlerThread: HandlerThread? = null

    private var subscribersCount = AtomicInteger()

    @Suppress("ReturnCount")
    fun provide(): HandlerThread {
        handlerThread?.let {
            subscribersCount.incrementAndGet()
            return it
        }
        synchronized(this) {
            handlerThread?.let {
                subscribersCount.incrementAndGet()
                return it
            }
            return HandlerThread(name).apply { start() }.also {
                handlerThread = it
                subscribersCount.incrementAndGet()
            }
        }
    }

    fun release() {
        handlerThread ?: run {
            subscribersCount.decrementAndGet()
            return
        }
        synchronized(this) {
            if (subscribersCount.decrementAndGet() == 0) {
                handlerThread?.quit()
                handlerThread = null
            }
        }
    }
}
