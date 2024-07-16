package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow

interface IServiceProvider {
    fun <T : IServiceRest> provideService(
        accountId: Long,
        serviceClass: T,
        vararg tokenTypes: Int
    ): Flow<T>
}