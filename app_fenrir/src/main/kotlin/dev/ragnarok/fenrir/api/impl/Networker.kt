package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.IDirectLoginSeviceProvider
import dev.ragnarok.fenrir.api.ILocalServerServiceProvider
import dev.ragnarok.fenrir.api.IOtherVKRestProvider
import dev.ragnarok.fenrir.api.IUploadRestProvider
import dev.ragnarok.fenrir.api.IVKRestProvider
import dev.ragnarok.fenrir.api.OtherVKRestProvider
import dev.ragnarok.fenrir.api.UploadRestProvider
import dev.ragnarok.fenrir.api.VKMethodHttpClientFactory
import dev.ragnarok.fenrir.api.VKRestProvider
import dev.ragnarok.fenrir.api.interfaces.IAccountApis
import dev.ragnarok.fenrir.api.interfaces.IAuthApi
import dev.ragnarok.fenrir.api.interfaces.ILocalServerApi
import dev.ragnarok.fenrir.api.interfaces.ILongpollApi
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.interfaces.IUploadApi
import dev.ragnarok.fenrir.api.services.IAuthService
import dev.ragnarok.fenrir.api.services.ILocalServerService
import dev.ragnarok.fenrir.settings.IProxySettings
import io.reactivex.rxjava3.core.Single

class Networker(settings: IProxySettings) : INetworker {
    private val otherVkRestProvider: IOtherVKRestProvider
    private val vkRestProvider: IVKRestProvider
    private val uploadRestProvider: IUploadRestProvider
    override fun vkDefault(accountId: Long): IAccountApis {
        return VKApies[accountId, vkRestProvider]
    }

    override fun vkManual(accountId: Long, accessToken: String): IAccountApis {
        return VKApies.create(accountId, accessToken, vkRestProvider)
    }

    override fun getVkRestProvider(): IVKRestProvider {
        return vkRestProvider
    }

    override fun vkDirectAuth(@AccountType accountType: Int, customDevice: String?): IAuthApi {
        return AuthApi(object : IDirectLoginSeviceProvider {
            override fun provideAuthService(): Single<IAuthService> {
                return otherVkRestProvider.provideAuthRest(accountType, customDevice)
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
        otherVkRestProvider = OtherVKRestProvider(settings)
        vkRestProvider = VKRestProvider(settings, VKMethodHttpClientFactory())
        uploadRestProvider = UploadRestProvider(settings)
    }
}