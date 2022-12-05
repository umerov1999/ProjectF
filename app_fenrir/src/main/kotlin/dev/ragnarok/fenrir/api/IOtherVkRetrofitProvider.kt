package dev.ragnarok.fenrir.api

import io.reactivex.rxjava3.core.Single

interface IOtherVkRetrofitProvider {
    fun provideAuthRetrofit(): Single<RetrofitWrapper>
    fun provideAuthServiceRetrofit(): Single<RetrofitWrapper>
    fun provideLongpollRetrofit(): Single<RetrofitWrapper>
    fun provideLocalServerRetrofit(): Single<RetrofitWrapper>
}