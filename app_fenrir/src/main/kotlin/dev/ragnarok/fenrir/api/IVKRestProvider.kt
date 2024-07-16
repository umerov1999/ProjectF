package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient

interface IVKRestProvider {
    fun provideNormalRest(accountId: Long): Flow<SimplePostHttp>
    fun provideCustomRest(accountId: Long, token: String): Flow<SimplePostHttp>
    fun provideServiceRest(): Flow<SimplePostHttp>
    fun provideNormalHttpClient(accountId: Long): Flow<OkHttpClient.Builder>
    fun provideRawHttpClient(
        @AccountType type: Int,
        customDeviceName: String?
    ): Flow<OkHttpClient.Builder>
}