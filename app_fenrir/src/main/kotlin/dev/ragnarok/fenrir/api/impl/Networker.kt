package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.*
import dev.ragnarok.fenrir.api.interfaces.*
import dev.ragnarok.fenrir.api.services.IAuthService
import dev.ragnarok.fenrir.api.services.ILocalServerService
import dev.ragnarok.fenrir.settings.IProxySettings
import io.reactivex.rxjava3.core.Single

class Networker(settings: IProxySettings) : INetworker {
    private val otherVkRetrofitProvider: IOtherVkRetrofitProvider
    private val vkRetrofitProvider: IVkRetrofitProvider
    private val uploadRetrofitProvider: IUploadRetrofitProvider
    override fun vkDefault(accountId: Int): IAccountApis {
        return VkApies[accountId, vkRetrofitProvider]
    }

    override fun vkManual(accountId: Int, accessToken: String): IAccountApis {
        return VkApies.create(accountId, accessToken, vkRetrofitProvider)
    }

    override fun getVkRetrofitProvider(): IVkRetrofitProvider {
        return vkRetrofitProvider
    }

    override fun vkDirectAuth(): IAuthApi {
        return AuthApi(object : IDirectLoginSeviceProvider {
            override fun provideAuthService(): Single<IAuthService> {
                return otherVkRetrofitProvider.provideAuthRetrofit()
                    .map { wrapper ->
                        wrapper.create(
                            IAuthService::class.java
                        )
                    }
            }
        })
    }

    override fun vkAuth(): IAuthApi {
        return AuthApi(object : IDirectLoginSeviceProvider {
            override fun provideAuthService(): Single<IAuthService> {
                return otherVkRetrofitProvider.provideAuthServiceRetrofit()
                    .map { wrapper ->
                        wrapper.create(
                            IAuthService::class.java
                        )
                    }
            }
        })
    }

    override fun localServerApi(): ILocalServerApi {
        return LocalServerApi(object : ILocalServerServiceProvider {
            override fun provideLocalServerService(): Single<ILocalServerService> {
                return otherVkRetrofitProvider.provideLocalServerRetrofit()
                    .map { wrapper ->
                        wrapper.create(
                            ILocalServerService::class.java
                        )
                    }
            }
        })
    }

    override fun longpoll(): ILongpollApi {
        return LongpollApi(otherVkRetrofitProvider)
    }

    override fun uploads(): IUploadApi {
        return UploadApi(uploadRetrofitProvider)
    }

    init {
        otherVkRetrofitProvider = OtherVkRetrofitProvider(settings)
        vkRetrofitProvider = VkRetrofitProvider(settings, VkMethodHttpClientFactory())
        uploadRetrofitProvider = UploadRetrofitProvider(settings)
    }
}