package dev.ragnarok.filegallery.util.serializeble.retrofit.rxjava3

import io.reactivex.rxjava3.core.*
import retrofit2.CallAdapter
import retrofit2.Response
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class RxJava3CallAdapterFactory private constructor(
    private val scheduler: Scheduler?,
    private val isAsync: Boolean
) : CallAdapter.Factory() {
    override fun get(
        returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit
    ): CallAdapter<*, *>? {
        val rawType = getRawType(returnType)
        if (rawType == Completable::class.java) {
            // Completable is not parameterized (which is what the rest of this method deals with) so it
            // can only be created with a single configuration.
            return RxJava3CallAdapter<Any>(
                Void::class.java, scheduler, isAsync,
                isResult = false,
                isBody = true,
                isFlowable = false,
                isSingle = false,
                isMaybe = false,
                isCompletable = true
            )
        }
        val isFlowable = rawType == Flowable::class.java
        val isSingle = rawType == Single::class.java
        val isMaybe = rawType == Maybe::class.java
        if (rawType != Observable::class.java && !isFlowable && !isSingle && !isMaybe) {
            return null
        }
        var isResult = false
        var isBody = false
        val responseType: Type
        if (returnType !is ParameterizedType) {
            val name =
                if (isFlowable) "Flowable" else if (isSingle) "Single" else if (isMaybe) "Maybe" else "Observable"
            throw IllegalStateException(
                name
                        + " return type must be parameterized"
                        + " as "
                        + name
                        + "<Foo> or "
                        + name
                        + "<? extends Foo>"
            )
        }
        val observableType = getParameterUpperBound(0, returnType)
        when (getRawType(observableType)) {
            Response::class.java -> {
                check(observableType is ParameterizedType) { "Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>" }
                responseType = getParameterUpperBound(0, observableType)
            }
            Result::class.java -> {
                check(observableType is ParameterizedType) { "Result must be parameterized" + " as Result<Foo> or Result<? extends Foo>" }
                responseType = getParameterUpperBound(0, observableType)
                isResult = true
            }
            else -> {
                responseType = observableType
                isBody = true
            }
        }
        return RxJava3CallAdapter<Any>(
            responseType, scheduler, isAsync, isResult, isBody, isFlowable, isSingle, isMaybe, false
        )
    }

    companion object {
        /**
         * Returns an instance which creates asynchronous observables that run on a background thread by
         * default. Applying `subscribeOn(..)` has no effect on instances created by the returned
         * factory.
         */
        fun create(): RxJava3CallAdapterFactory {
            return RxJava3CallAdapterFactory(null, true)
        }

        /**
         * Returns an instance which creates synchronous observables that do not operate on any scheduler
         * by default. Applying `subscribeOn(..)` will change the scheduler on which the HTTP calls
         * are made.
         */
        fun createSynchronous(): RxJava3CallAdapterFactory {
            return RxJava3CallAdapterFactory(null, false)
        }

        /**
         * Returns an instance which creates synchronous observables that `subscribeOn(..)` the
         * supplied `scheduler` by default.
         */
        // Guarding public API nullability.
        fun createWithScheduler(scheduler: Scheduler?): RxJava3CallAdapterFactory {
            if (scheduler == null) throw NullPointerException("scheduler == null")
            return RxJava3CallAdapterFactory(scheduler, false)
        }
    }
}