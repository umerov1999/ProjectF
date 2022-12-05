package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.api.services.ILocalServerService
import io.reactivex.rxjava3.core.Single

interface ILocalServerServiceProvider {
    fun provideLocalServerService(): Single<ILocalServerService>
}