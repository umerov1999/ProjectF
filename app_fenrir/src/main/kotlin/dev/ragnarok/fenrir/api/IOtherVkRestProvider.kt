package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import io.reactivex.rxjava3.core.Single

interface IOtherVkRestProvider {
    fun provideAuthRest(): Single<SimplePostHttp>
    fun provideAuthServiceRest(): Single<SimplePostHttp>
    fun provideLongpollRest(): Single<SimplePostHttp>
    fun provideLocalServerRest(): Single<SimplePostHttp>
}