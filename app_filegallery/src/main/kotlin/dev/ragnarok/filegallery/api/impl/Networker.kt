package dev.ragnarok.filegallery.api.impl

import dev.ragnarok.filegallery.api.ILocalServerServiceProvider
import dev.ragnarok.filegallery.api.IOtherRestProvider
import dev.ragnarok.filegallery.api.OtherRestProvider
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.api.interfaces.INetworker
import dev.ragnarok.filegallery.api.services.ILocalServerService
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings
import io.reactivex.rxjava3.core.Single

class Networker(settings: IMainSettings) : INetworker {
    private val otherVkRestProvider: IOtherRestProvider
    override fun localServerApi(): ILocalServerApi {
        return LocalServerApi(object : ILocalServerServiceProvider {
            override fun provideLocalServerService(): Single<ILocalServerService> {
                return otherVkRestProvider.provideLocalServerRest()
                    .map {
                        val ret = ILocalServerService()
                        ret.addon(it)
                        ret
                    }
            }
        })
    }

    init {
        otherVkRestProvider = OtherRestProvider(settings)
    }
}