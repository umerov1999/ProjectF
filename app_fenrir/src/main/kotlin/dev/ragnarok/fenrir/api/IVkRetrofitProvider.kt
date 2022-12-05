package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import io.reactivex.rxjava3.core.Single
import okhttp3.OkHttpClient

interface IVkRetrofitProvider {
    fun provideNormalRetrofit(accountId: Int): Single<RetrofitWrapper>
    fun provideCustomRetrofit(accountId: Int, token: String): Single<RetrofitWrapper>
    fun provideServiceRetrofit(): Single<RetrofitWrapper>
    fun provideNormalHttpClient(accountId: Int): Single<OkHttpClient>
    fun provideRawHttpClient(@AccountType type: Int): Single<OkHttpClient>
}