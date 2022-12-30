package dev.ragnarok.fenrir.api.interfaces

import dev.ragnarok.fenrir.api.IVkRestProvider

interface INetworker {
    fun getVkRestProvider(): IVkRestProvider
    fun vkDefault(accountId: Int): IAccountApis
    fun vkManual(accountId: Int, accessToken: String): IAccountApis
    fun vkDirectAuth(): IAuthApi
    fun vkAuth(): IAuthApi
    fun localServerApi(): ILocalServerApi
    fun longpoll(): ILongpollApi
    fun uploads(): IUploadApi
}