package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import io.reactivex.rxjava3.core.Single
import okhttp3.OkHttpClient

interface IVkRestProvider {
    fun provideNormalRest(accountId: Int): Single<SimplePostHttp>
    fun provideCustomRest(accountId: Int, token: String): Single<SimplePostHttp>
    fun provideServiceRest(): Single<SimplePostHttp>
    fun provideNormalHttpClient(accountId: Int): Single<OkHttpClient.Builder>
    fun provideRawHttpClient(@AccountType type: Int): Single<OkHttpClient.Builder>
}