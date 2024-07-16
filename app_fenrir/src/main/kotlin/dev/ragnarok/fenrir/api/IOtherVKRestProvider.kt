package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import kotlinx.coroutines.flow.Flow

interface IOtherVKRestProvider {
    fun provideAuthRest(
        @AccountType accountType: Int,
        customDevice: String?
    ): Flow<SimplePostHttp>

    fun provideAuthServiceRest(): Flow<SimplePostHttp>
    fun provideLongpollRest(): Flow<SimplePostHttp>
    fun provideLocalServerRest(): Flow<SimplePostHttp>
}