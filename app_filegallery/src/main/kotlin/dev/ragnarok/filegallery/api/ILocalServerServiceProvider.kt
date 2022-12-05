package dev.ragnarok.filegallery.api

import dev.ragnarok.filegallery.api.services.ILocalServerService
import io.reactivex.rxjava3.core.Single

interface ILocalServerServiceProvider {
    fun provideLocalServerService(): Single<ILocalServerService>
}