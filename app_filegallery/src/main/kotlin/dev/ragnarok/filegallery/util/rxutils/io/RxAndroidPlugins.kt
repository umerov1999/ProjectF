package dev.ragnarok.filegallery.util.rxutils.io

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.functions.Function
import java.util.concurrent.Callable

/**
 * Utility class to inject handlers to certain standard RxAndroid operations.
 */
class RxAndroidPlugins private constructor() {
    companion object {
        /**
         * Returns the current hook function.
         * @return the hook function, may be null
         */
        @Volatile
        var initMainThreadSchedulerHandler: Function<Callable<Scheduler?>, Scheduler>? = null

        /**
         * Returns the current hook function.
         * @return the hook function, may be null
         */
        @Volatile
        var onMainThreadSchedulerHandler: Function<Scheduler, Scheduler>? = null
            private set

        fun initMainThreadScheduler(scheduler: Callable<Scheduler?>?): Scheduler {
            if (scheduler == null) {
                throw NullPointerException("scheduler == null")
            }
            val f = initMainThreadSchedulerHandler
                ?: return callRequireNonNull(scheduler)
            return applyRequireNonNull(f, scheduler)
        }

        fun setMainThreadSchedulerHandler(handler: Function<Scheduler, Scheduler>?) {
            onMainThreadSchedulerHandler = handler
        }

        fun onMainThreadScheduler(scheduler: Scheduler?): Scheduler {
            if (scheduler == null) {
                throw NullPointerException("scheduler == null")
            }
            val f = onMainThreadSchedulerHandler ?: return scheduler
            return apply(f, scheduler)
        }

        /**
         * Removes all handlers and resets the default behavior.
         */
        fun reset() {
            initMainThreadSchedulerHandler = null
            setMainThreadSchedulerHandler(null)
        }

        private fun callRequireNonNull(s: Callable<Scheduler?>): Scheduler {
            return try {
                val scheduler = s.call()
                    ?: throw NullPointerException("Scheduler Callable returned null")
                scheduler
            } catch (ex: Throwable) {
                throw Exceptions.propagate(ex)
            }
        }

        private fun applyRequireNonNull(
            f: Function<Callable<Scheduler?>, Scheduler>,
            s: Callable<Scheduler?>
        ): Scheduler {
            return apply(
                f,
                s
            )
        }

        inline fun <reified T : Any, reified R : Any> apply(f: Function<T, R>, t: T): R {
            return try {
                f.apply(t)
            } catch (ex: Throwable) {
                throw Exceptions.propagate(ex)
            }
        }
    }

    init {
        throw AssertionError("No instances.")
    }
}