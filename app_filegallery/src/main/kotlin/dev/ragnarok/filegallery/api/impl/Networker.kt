package dev.ragnarok.filegallery.api.impl

import dev.ragnarok.filegallery.api.ILocalServerRestProvider
import dev.ragnarok.filegallery.api.ILocalServerServiceProvider
import dev.ragnarok.filegallery.api.IUploadRestProvider
import dev.ragnarok.filegallery.api.LocalServerRestProvider
import dev.ragnarok.filegallery.api.UploadRestProvider
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.api.interfaces.INetworker
import dev.ragnarok.filegallery.api.interfaces.IUploadApi
import dev.ragnarok.filegallery.api.services.ILocalServerService
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings
import io.reactivex.rxjava3.core.Single

class Networker(settings: IMainSettings) : INetworker {
    private val localServerRestProvider: ILocalServerRestProvider
    private val uploadRestProvider: IUploadRestProvider
    override fun localServerApi(): ILocalServerApi {
        return LocalServerApi(object : ILocalServerServiceProvider {
            override fun provideLocalServerService(): Single<ILocalServerService> {
                return localServerRestProvider.provideLocalServerRest()
                    .map {
                        val ret = ILocalServerService()
                        ret.addon(it)
                        ret
                    }
            }
        })
    }

    override fun uploads(): IUploadApi {
        return UploadApi(uploadRestProvider)
    }

    init {
        localServerRestProvider = LocalServerRestProvider(settings)
        uploadRestProvider = UploadRestProvider(settings)
    }
}