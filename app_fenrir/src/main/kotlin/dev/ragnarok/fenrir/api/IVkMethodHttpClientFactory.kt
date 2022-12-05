package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.model.ProxyConfig
import okhttp3.OkHttpClient

interface IVkMethodHttpClientFactory {
    fun createDefaultVkHttpClient(accountId: Int, config: ProxyConfig?): OkHttpClient
    fun createCustomVkHttpClient(
        accountId: Int,
        token: String,
        config: ProxyConfig?
    ): OkHttpClient

    fun createServiceVkHttpClient(config: ProxyConfig?): OkHttpClient
    fun createRawVkApiOkHttpClient(@AccountType type: Int, config: ProxyConfig?): OkHttpClient
}