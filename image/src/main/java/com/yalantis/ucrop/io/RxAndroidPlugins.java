package com.yalantis.ucrop.io;

import java.util.concurrent.Callable;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.exceptions.Exceptions;
import io.reactivex.rxjava3.functions.Function;

/**
 * Utility class to inject handlers to certain standard RxAndroid operations.
 */
public final class RxAndroidPlugins {

    private static volatile Function<Callable<Scheduler>, Scheduler> onInitMainThreadHandler;
    private static volatile Function<Scheduler, Scheduler> onMainThreadHandler;

    private RxAndroidPlugins() {
        throw new AssertionError("No instances.");
    }

    public static Scheduler initMainThreadScheduler(Callable<Scheduler> scheduler) {
        if (scheduler == null) {
            throw new NullPointerException("scheduler == null");
        }
        Function<Callable<Scheduler>, Scheduler> f = onInitMainThreadHandler;
        if (f == null) {
            return callRequireNonNull(scheduler);
        }
        return applyRequireNonNull(f, scheduler);
    }

    public static void setMainThreadSchedulerHandler(Function<Scheduler, Scheduler> handler) {
        onMainThreadHandler = handler;
    }

    public static Scheduler onMainThreadScheduler(Scheduler scheduler) {
        if (scheduler == null) {
            throw new NullPointerException("scheduler == null");
        }
        Function<Scheduler, Scheduler> f = onMainThreadHandler;
        if (f == null) {
            return scheduler;
        }
        return apply(f, scheduler);
    }

    /**
     * Returns the current hook function.
     *
     * @return the hook function, may be null
     */
    public static Function<Callable<Scheduler>, Scheduler> getInitMainThreadSchedulerHandler() {
        return onInitMainThreadHandler;
    }

    public static void setInitMainThreadSchedulerHandler(Function<Callable<Scheduler>, Scheduler> handler) {
        onInitMainThreadHandler = handler;
    }

    /**
     * Returns the current hook function.
     *
     * @return the hook function, may be null
     */
    public static Function<Scheduler, Scheduler> getOnMainThreadSchedulerHandler() {
        return onMainThreadHandler;
    }

    /**
     * Removes all handlers and resets the default behavior.
     */
    public static void reset() {
        setInitMainThreadSchedulerHandler(null);
        setMainThreadSchedulerHandler(null);
    }

    static Scheduler callRequireNonNull(Callable<Scheduler> s) {
        try {
            Scheduler scheduler = s.call();
            if (scheduler == null) {
                throw new NullPointerException("Scheduler Callable returned null");
            }
            return scheduler;
        } catch (Throwable ex) {
            throw Exceptions.propagate(ex);
        }
    }

    static Scheduler applyRequireNonNull(Function<Callable<Scheduler>, Scheduler> f, Callable<Scheduler> s) {
        Scheduler scheduler = apply(f, s);
        if (scheduler == null) {
            throw new NullPointerException("Scheduler Callable returned null");
        }
        return scheduler;
    }

    static <T, R> R apply(Function<T, R> f, T t) {
        try {
            return f.apply(t);
        } catch (Throwable ex) {
            throw Exceptions.propagate(ex);
        }
    }
}
