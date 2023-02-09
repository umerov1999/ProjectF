package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import io.reactivex.rxjava3.core.Single

interface IOtherVKRestProvider {
    fun provideAuthRest(
        @AccountType accountType: Int,
        customDevice: String?
    ): Single<SimplePostHttp>

    fun provideAuthServiceRest(): Single<SimplePostHttp>
    fun provideLongpollRest(): Single<SimplePostHttp>
    fun provideLocalServerRest(): Single<SimplePostHttp>
}