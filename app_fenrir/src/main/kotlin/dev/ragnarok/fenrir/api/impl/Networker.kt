package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.api.*
import dev.ragnarok.fenrir.api.interfaces.*
import dev.ragnarok.fenrir.api.services.IAuthService
import dev.ragnarok.fenrir.api.services.ILocalServerService
import dev.ragnarok.fenrir.settings.IProxySettings
import io.reactivex.rxjava3.core.Single

class Networker(settings: IProxySettings) : INetworker {
    private val otherVkRestProvider: IOtherVkRestProvider
    private val vkRestProvider: IVkRestProvider
    private val uploadRestProvider: IUploadRestProvider
    override fun vkDefault(accountId: Long): IAccountApis {
        return VkApies[accountId, vkRestProvider]
    }

    override fun vkManual(accountId: Long, accessToken: String): IAccountApis {
        return VkApies.create(accountId, accessToken, vkRestProvider)
    }

    override fun getVkRestProvider(): IVkRestProvider {
        return vkRestProvider
    }

    override fun vkDirectAuth(): IAuthApi {
        return AuthApi(object : IDirectLoginSeviceProvider {
            override fun provideAuthService(): Single<IAuthService> {
                return otherVkRestProvider.provideAuthRest()
                    .map {
                        val ret = IAuthService()
                        ret.addon(it)
                        ret
                    }
            }
        })
    }

    override fun vkAuth(): IAuthApi {
        return AuthApi(object : IDirectLoginSeviceProvider {
            override fun provideAuthService(): Single<IAuthService> {
                return otherVkRestProvider.provideAuthServiceRest()
                    .map {
                        val ret = IAuthService()
                        ret.addon(it)
                        ret
                    }
            }
        })
    }

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

    override fun longpoll(): ILongpollApi {
        return LongpollApi(otherVkRestProvider)
    }

    override fun uploads(): IUploadApi {
        return UploadApi(uploadRestProvider)
    }

    init {
        otherVkRestProvider = OtherVkRestProvider(settings)
        vkRestProvider = VkRestProvider(settings, VkMethodHttpClientFactory())
        uploadRestProvider = UploadRestProvider(settings)
    }
}