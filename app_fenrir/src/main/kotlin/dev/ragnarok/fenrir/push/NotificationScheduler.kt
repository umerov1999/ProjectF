package dev.ragnarok.fenrir.push

import dev.ragnarok.fenrir.Includes.provideMainThreadScheduler
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleTransformer
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Executors

object NotificationScheduler {
    val INSTANCE: Scheduler = Schedulers.from(Executors.newFixedThreadPool(1))
    fun <T : Any> fromNotificationThreadToMain(): SingleTransformer<T, T> {
        return SingleTransformer { single: Single<T> ->
            single
                .subscribeOn(INSTANCE)
                .observeOn(provideMainThreadScheduler())
        }
    }
}