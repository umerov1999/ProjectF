package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import io.reactivex.rxjava3.core.Single

interface IUploadRestProvider {
    fun provideUploadRest(): Single<SimplePostHttp>
}