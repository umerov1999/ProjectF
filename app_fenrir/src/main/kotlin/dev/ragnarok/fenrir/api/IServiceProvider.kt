package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.rest.IServiceRest
import io.reactivex.rxjava3.core.Single

interface IServiceProvider {
    fun <T : IServiceRest> provideService(
        accountId: Long,
        serviceClass: T,
        vararg tokenTypes: Int
    ): Single<T>
}