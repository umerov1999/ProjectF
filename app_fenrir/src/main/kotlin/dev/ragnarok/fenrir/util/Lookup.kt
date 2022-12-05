package dev.ragnarok.fenrir.util

import android.os.Handler
import android.os.Looper

class Lookup(private var mDelay: Int) {
    private val mHandler: Handler
    private var mCallback: Callback? = null
    private fun onLookupHandle() {
        mHandler.sendEmptyMessageDelayed(LOOKUP, mDelay.toLong())
        mCallback?.onIterated()
    }

    fun changeDelayTime(delay: Int, startNow: Boolean) {
        mDelay = delay
        if (startNow) {
            mHandler.removeMessages(LOOKUP)
            mHandler.sendEmptyMessageDelayed(LOOKUP, mDelay.toLong())
        }
    }

    fun stop() {
        mHandler.removeMessages(LOOKUP)
    }

    fun start() {
        if (!mHandler.hasMessages(LOOKUP)) {
            mHandler.sendEmptyMessageDelayed(LOOKUP, mDelay.toLong())
        }
    }

    fun setCallback(callback: Callback?) {
        mCallback = callback
    }

    interface Callback {
        fun onIterated()
    }

    companion object {
        private const val LOOKUP = 1540
    }

    init {
        mHandler = Handler(Looper.getMainLooper()) {
            onLookupHandle()
            true
        }
    }
}