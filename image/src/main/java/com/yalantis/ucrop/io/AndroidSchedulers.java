package com.yalantis.ucrop.io;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import io.reactivex.rxjava3.core.Scheduler;

/**
 * Android-specific Schedulers.
 */
public final class AndroidSchedulers {

    private static final Scheduler MAIN_THREAD =
            RxAndroidPlugins.initMainThreadScheduler(() -> MainHolder.DEFAULT);

    private AndroidSchedulers() {
        throw new AssertionError("No instances.");
    }

    /**
     * A {@link Scheduler} which executes actions on the Android main thread.
     * <p>
     * The returned scheduler will post asynchronous messages to the looper by default.
     *
     * @see #from(Looper, boolean)
     */
    public static Scheduler mainThread() {
        return RxAndroidPlugins.onMainThreadScheduler(MAIN_THREAD);
    }

    /**
     * A {@link Scheduler} which executes actions on {@code looper}.
     * <p>
     * The returned scheduler will post asynchronous messages to the looper by default.
     *
     * @see #from(Looper, boolean)
     */
    public static Scheduler from(Looper looper) {
        return from(looper, true);
    }

    /**
     * A {@link Scheduler} which executes actions on {@code looper}.
     *
     * @param async if true, the scheduler will use async messaging on API >= 16 to avoid VSYNC
     *              locking. On API < 16 this value is ignored.
     * @see Message#setAsynchronous(boolean)
     */
    @SuppressLint("NewApi") // Checking for an @hide API.
    public static Scheduler from(Looper looper, boolean async) {
        if (looper == null) throw new NullPointerException("looper == null");

        // Below code exists in androidx-core as well, but is left here rather than include an
        // entire extra dependency.
        // https://developer.android.com/reference/kotlin/androidx/core/os/MessageCompat?hl=en#setAsynchronous(android.os.Message,%20kotlin.Boolean)
        if (async && Build.VERSION.SDK_INT < 22) {
            // Confirm that the method is available on this API level despite being @hide.
            Message message = Message.obtain();
            try {
                message.setAsynchronous(true);
            } catch (NoSuchMethodError e) {
                async = false;
            }
            message.recycle();
        }
        return new HandlerScheduler(new Handler(looper), async);
    }

    private static final class MainHolder {
        static final Scheduler DEFAULT
                = from(Looper.getMainLooper());
    }
}
