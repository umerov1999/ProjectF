package dev.ragnarok.fenrir.util.rxutils

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.Closeable

object RxUtils {
    private val DUMMMY_ACTION_0 = Action {}
    fun dummy(): Action {
        return DUMMMY_ACTION_0
    }

    inline fun <reified T : Any> ignore(): Consumer<T> {
        return Consumer { t: T ->
            if (t is Throwable && Constants.IS_DEBUG) {
                (t as Throwable).printStackTrace()
            }
        }
    }

    fun safelyCloseAction(closeable: Closeable?): Action {
        return Action { Utils.safelyClose(closeable) }
    }


    inline fun <reified T : Any> subscribeOnIOAndIgnore(single: Single<T>): Disposable {
        return single.subscribeOn(Schedulers.io())
            .subscribe(ignore(), ignore())
    }


    fun subscribeOnIOAndIgnore(completable: Completable): Disposable {
        return completable.subscribeOn(Schedulers.io())
            .subscribe(dummy(), ignore())
    }

    inline fun <reified T : Any> blockingGetSingle(single: Single<T>, default_value: T?): T? {
        try {
            return single.blockingGet()
        } catch (e: Throwable) {
            if (Constants.IS_DEBUG) {
                e.printStackTrace()
            }
        }
        return default_value
    }
}
