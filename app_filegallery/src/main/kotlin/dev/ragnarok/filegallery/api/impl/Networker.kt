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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Networker(settings: IMainSettings) : INetworker {
    private val localServerRestProvider: ILocalServerRestProvider =
        LocalServerRestProvider(settings)
    private val uploadRestProvider: IUploadRestProvider = UploadRestProvider(settings)
    override fun localServerApi(): ILocalServerApi {
        return LocalServerApi(object : ILocalServerServiceProvider {
            override fun provideLocalServerService(): Flow<ILocalServerService> {
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
}