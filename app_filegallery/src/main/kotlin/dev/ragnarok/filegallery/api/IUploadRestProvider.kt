package dev.ragnarok.filegallery.api

import dev.ragnarok.filegallery.api.rest.SimplePostHttp
import io.reactivex.rxjava3.core.Single

interface IUploadRestProvider {
    fun provideUploadRest(): Single<SimplePostHttp>
}