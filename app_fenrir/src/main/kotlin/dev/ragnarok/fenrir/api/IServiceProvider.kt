package dev.ragnarok.fenrir.api

import io.reactivex.rxjava3.core.Single

interface IServiceProvider {
    fun <T : Any> provideService(
        accountId: Int,
        serviceClass: Class<T>,
        vararg tokenTypes: Int
    ): Single<T>
}