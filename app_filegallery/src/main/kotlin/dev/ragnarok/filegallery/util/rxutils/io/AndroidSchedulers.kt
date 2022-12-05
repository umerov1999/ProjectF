package dev.ragnarok.filegallery.util.rxutils.io

import android.annotation.SuppressLint
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import io.reactivex.rxjava3.core.Scheduler

/** Android-specific Schedulers.  */
class AndroidSchedulers private constructor() {
    private object MainHolder {
        val DEFAULT = from(Looper.getMainLooper())
    }

    companion object {
        private val MAIN_THREAD: Scheduler =
            RxAndroidPlugins.initMainThreadScheduler { MainHolder.DEFAULT }

        /**
         * A [Scheduler] which executes actions on the Android main thread.
         *
         *
         * The returned scheduler will post asynchronous messages to the looper by default.
         *
         * @see .from
         */
        fun mainThread(): Scheduler {
            return RxAndroidPlugins.onMainThreadScheduler(MAIN_THREAD)
        }

        /**
         * A [Scheduler] which executes actions on `looper`.
         *
         *
         * The returned scheduler will post asynchronous messages to the looper by default.
         *
         * @see .from
         */
        fun from(looper: Looper?): Scheduler {
            return from(looper, true)
        }

        /**
         * A [Scheduler] which executes actions on `looper`.
         *
         * @param async if true, the scheduler will use async messaging on API >= 16 to avoid VSYNC
         * locking. On API < 16 this value is ignored.
         * @see Message.setAsynchronous
         */
        @SuppressLint("NewApi") // Checking for an @hide API.
        fun from(looper: Looper?, async: Boolean): Scheduler {
            var pAsync = async
            if (looper == null) throw NullPointerException("looper == null")

            // Below code exists in androidx-core as well, but is left here rather than include an
            // entire extra dependency.
            // https://developer.android.com/reference/kotlin/androidx/core/os/MessageCompat?hl=en#setAsynchronous(android.os.Message,%20kotlin.Boolean)
            if (pAsync && Build.VERSION.SDK_INT < 22) {
                // Confirm that the method is available on this API level despite being @hide.
                val message = Message.obtain()
                try {
                    message.isAsynchronous = true
                } catch (e: NoSuchMethodError) {
                    pAsync = false
                }
                message.recycle()
            }
            return HandlerScheduler(Handler(looper), pAsync)
        }
    }

    init {
        throw AssertionError("No instances.")
    }
}