package dev.ragnarok.fenrir.util

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import androidx.annotation.WorkerThread
import kotlin.concurrent.Volatile

abstract class IntentService(private val mName: String) : Service() {
    @Volatile
    private var mServiceLooper: Looper? = null

    @Volatile
    private var mServiceHandler: ServiceHandler? = null
    private var mRedelivery = false

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            onHandleIntent(msg.obj as Intent)
            stopSelf(msg.arg1)
        }
    }

    fun setIntentRedelivery(enabled: Boolean) {
        mRedelivery = enabled
    }

    override fun onCreate() {
        super.onCreate()
        val thread = HandlerThread("IntentService[$mName]")
        thread.start()

        mServiceLooper = thread.looper
        mServiceLooper?.let {
            mServiceHandler = ServiceHandler(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val msg = mServiceHandler?.obtainMessage()
            ?: return if (mRedelivery) START_REDELIVER_INTENT else START_NOT_STICKY
        msg.arg1 = startId
        msg.obj = intent
        mServiceHandler?.sendMessage(msg)

        return if (mRedelivery) START_REDELIVER_INTENT else START_NOT_STICKY
    }

    override fun onDestroy() {
        mServiceLooper?.quit()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @WorkerThread
    protected abstract fun onHandleIntent(intent: Intent?)
}
