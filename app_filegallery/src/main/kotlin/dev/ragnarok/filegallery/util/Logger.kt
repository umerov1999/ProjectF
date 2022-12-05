package dev.ragnarok.filegallery.util

import android.util.Log
import dev.ragnarok.filegallery.Constants

object Logger {
    fun i(tag: String?, message: String) {
        if (Constants.IS_DEBUG) {
            Log.i(tag, message)
        }
    }

    fun d(tag: String?, message: String) {
        if (Constants.IS_DEBUG) {
            Log.d(tag, message)
        }
    }

    fun e(tag: String?, message: String) {
        if (Constants.IS_DEBUG) {
            Log.e(tag, message)
        }
    }

    fun wtf(tag: String?, message: String) {
        if (Constants.IS_DEBUG) {
            Log.wtf(tag, message)
        }
    }
}