package dev.ragnarok.filegallery.api

import io.reactivex.rxjava3.core.Single

interface IOtherRetrofitProvider {
    fun provideLocalServerRetrofit(): Single<RetrofitWrapper>
}