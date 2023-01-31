package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.IVKRestProvider

interface INetworker {
    fun getVkRestProvider(): IVKRestProvider
    fun vkDefault(accountId: Long): IAccountApis
    fun vkManual(accountId: Long, accessToken: String): IAccountApis
    fun vkDirectAuth(
        @AccountType accountType: Int = Constants.DEFAULT_ACCOUNT_TYPE,
        customDevice: String? = null
    ): IAuthApi

    fun vkAuth(): IAuthApi
    fun localServerApi(): ILocalServerApi
    fun longpoll(): ILongpollApi
    fun uploads(): IUploadApi
}