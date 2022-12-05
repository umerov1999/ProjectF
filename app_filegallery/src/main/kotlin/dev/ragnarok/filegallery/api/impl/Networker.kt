package dev.ragnarok.filegallery.api.impl

import dev.ragnarok.filegallery.api.ILocalServerServiceProvider
import dev.ragnarok.filegallery.api.IOtherRetrofitProvider
import dev.ragnarok.filegallery.api.OtherRetrofitProvider
import dev.ragnarok.filegallery.api.RetrofitWrapper
import dev.ragnarok.filegallery.api.interfaces.ILocalServerApi
import dev.ragnarok.filegallery.api.interfaces.INetworker
import dev.ragnarok.filegallery.api.services.ILocalServerService
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings
import io.reactivex.rxjava3.core.Single

class Networker(settings: IMainSettings) : INetworker {
    private val otherVkRetrofitProvider: IOtherRetrofitProvider
    override fun localServerApi(): ILocalServerApi {
        return LocalServerApi(object : ILocalServerServiceProvider {
            override fun provideLocalServerService(): Single<ILocalServerService> {
                return otherVkRetrofitProvider.provideLocalServerRetrofit()
                    .map { wrapper: RetrofitWrapper ->
                        wrapper.create(
                            ILocalServerService::class.java
                        )
                    }
            }
        })
    }

    init {
        otherVkRetrofitProvider = OtherRetrofitProvider(settings)
    }
}