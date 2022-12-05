package dev.ragnarok.filegallery.util.rxutils.io

import android.os.Looper
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [disposable][Disposable] which ensures its [ dispose action][.onDispose] is executed on the main thread. When unsubscription occurs on a different
 * thread than the main thread, the action is posted to run on the main thread as soon as possible.
 *
 *
 * Instances of this class are useful in creating observables which interact with APIs that can
 * only be used on the main thread, such as UI objects.
 *
 *
 * A [convenience method][.verifyMainThread] is also provided for validating whether code
 * is being called on the main thread. Calls to this method along with instances of this class are
 * commonly used when creating custom observables using the following pattern:
 * <pre>`
 * &#064;Override public void subscribe(Observer o) {
 * MainThreadDisposable.verifyMainThread();
 *
 * // TODO setup behavior
 *
 * o.onSubscribe(new MainThreadDisposable() {
 * &#064;Override protected void onDispose() {
 * // TODO undo behavior
 * }
 * });
 * }
`</pre> *
 */
abstract class MainThreadDisposable : Disposable {
    private val unsubscribed = AtomicBoolean()
    override fun isDisposed(): Boolean {
        return unsubscribed.get()
    }

    override fun dispose() {
        if (unsubscribed.compareAndSet(false, true)) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                onDispose()
            } else {
                AndroidSchedulers.mainThread().scheduleDirect { onDispose() }
            }
        }
    }

    protected abstract fun onDispose()

    companion object {
        /**
         * Verify that the calling thread is the Android main thread.
         *
         *
         * Calls to this method are usually preconditions for subscription behavior which instances of
         * this class later undo. See the class documentation for an example.
         *
         * @throws IllegalStateException when called from any other thread.
         */
        fun verifyMainThread() {
            check(Looper.myLooper() == Looper.getMainLooper()) { "Expected to be called on the main thread but was " + Thread.currentThread().name }
        }
    }
}