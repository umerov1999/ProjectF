package dev.ragnarok.fenrir.module

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import java.util.concurrent.CountDownLatch

class DispatchQueue @JvmOverloads constructor(threadName: String, start: Boolean = true) :
    Thread() {
    private val syncLatch = CountDownLatch(1)

    @Volatile
    private var handler: Handler? = null
    var lastTaskTime: Long = 0
        private set

    fun sendMessage(msg: Message, delay: Int) {
        try {
            syncLatch.await()
            if (delay <= 0) {
                handler?.sendMessage(msg)
            } else {
                handler?.sendMessageDelayed(msg, delay.toLong())
            }
        } catch (ignore: Exception) {
        }
    }

    fun cancelRunnable(runnable: Runnable) {
        try {
            syncLatch.await()
            handler?.removeCallbacks(runnable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelRunnables(runnables: Array<Runnable>) {
        try {
            syncLatch.await()
            for (runnable in runnables) {
                handler?.removeCallbacks(runnable)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun postRunnable(runnable: Runnable): Boolean {
        lastTaskTime = SystemClock.elapsedRealtime()
        return postRunnable(runnable, 0)
    }

    fun postRunnable(runnable: Runnable, delay: Long): Boolean {
        try {
            syncLatch.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (delay <= 0) {
            handler?.post(runnable) == true
        } else {
            handler?.postDelayed(runnable, delay) == true
        }
    }

    fun cleanupQueue() {
        try {
            syncLatch.await()
            handler?.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun handleMessage(inputMessage: Message?) {
    }

    fun recycle() {
        handler?.looper?.quit()
    }

    @SuppressLint("HandlerLeak")
    override fun run() {
        Looper.prepare()
        handler = object : Handler(Looper.myLooper() ?: Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                this@DispatchQueue.handleMessage(msg)
            }
        }
        syncLatch.countDown()
        Looper.loop()
    }

    companion object {
        @Volatile
        private var applicationHandler: Handler? = null

        @Volatile
        private var globalQueue: DispatchQueue? = null
        fun getGlobalQueue(): DispatchQueue? {
            if (globalQueue == null) {
                globalQueue = DispatchQueue("globalQueue")
            }
            return globalQueue
        }

        @JvmOverloads
        fun runOnUIThread(runnable: Runnable, delay: Long = 0) {
            if (applicationHandler == null) applicationHandler = Handler(Looper.getMainLooper())
            if (delay == 0L) {
                applicationHandler?.post(
                    runnable
                )
            } else {
                applicationHandler?.postDelayed(
                    runnable, delay
                )
            }
        }
    }

    init {
        name = threadName
        if (start) {
            start()
        }
    }
}