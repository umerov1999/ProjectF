package dev.ragnarok.filegallery.util.serializeble.retrofit.rxjava3

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.CompositeException
import io.reactivex.rxjava3.exceptions.Exceptions
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import retrofit2.Call
import retrofit2.Response

internal class CallExecuteObservable<T>(private val originalCall: Call<T>) :
    Observable<Response<T>>() {
    override fun subscribeActual(observer: Observer<in Response<T>>) {
        // Since Call is a one-shot type, clone it for each new observer.
        val call = originalCall.clone()
        val disposable = CallDisposable(call)
        observer.onSubscribe(disposable)
        if (disposable.isDisposed) {
            return
        }
        var terminated = false
        try {
            val response = call.execute()
            if (!disposable.isDisposed) {
                observer.onNext(response)
            }
            if (!disposable.isDisposed) {
                terminated = true
                observer.onComplete()
            }
        } catch (t: Throwable) {
            Exceptions.throwIfFatal(t)
            if (terminated) {
                RxJavaPlugins.onError(t)
            } else if (!disposable.isDisposed) {
                try {
                    observer.onError(t)
                } catch (inner: Throwable) {
                    Exceptions.throwIfFatal(inner)
                    RxJavaPlugins.onError(CompositeException(t, inner))
                }
            }
        }
    }

    private class CallDisposable(private val call: Call<*>) : Disposable {
        @Volatile
        private var disposed = false
        override fun dispose() {
            disposed = true
            call.cancel()
        }

        override fun isDisposed(): Boolean {
            return disposed
        }
    }
}