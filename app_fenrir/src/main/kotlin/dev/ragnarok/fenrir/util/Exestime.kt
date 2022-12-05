package dev.ragnarok.fenrir.util

import android.util.Log
import dev.ragnarok.fenrir.Constants

object Exestime {
    private val TAG = Exestime::class.java.simpleName


    fun log(method: String, startTime: Long, vararg params: Any) {
        if (!Constants.IS_DEBUG) return
        if (params.isEmpty()) {
            Log.d(TAG, method + ", time: " + (System.currentTimeMillis() - startTime) + " ms")
        } else {
            Log.d(
                TAG,
                method + ", time: " + (System.currentTimeMillis() - startTime) + " ms, params: [" + Utils.join(
                    ", ",
                    params.asIterable()
                ) + "]"
            )
        }
    }
}