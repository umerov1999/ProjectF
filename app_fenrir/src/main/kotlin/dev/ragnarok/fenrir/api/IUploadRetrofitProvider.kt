package dev.ragnarok.fenrir.api

import io.reactivex.rxjava3.core.Single

interface IUploadRetrofitProvider {
    fun provideUploadRetrofit(): Single<RetrofitWrapper>
}