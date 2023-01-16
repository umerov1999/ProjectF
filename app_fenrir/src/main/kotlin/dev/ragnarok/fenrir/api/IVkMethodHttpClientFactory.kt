package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.model.ProxyConfig
import okhttp3.OkHttpClient

interface IVkMethodHttpClientFactory {
    fun createDefaultVkHttpClient(accountId: Long, config: ProxyConfig?): OkHttpClient.Builder
    fun createCustomVkHttpClient(
        accountId: Long,
        token: String,
        config: ProxyConfig?
    ): OkHttpClient.Builder

    fun createServiceVkHttpClient(config: ProxyConfig?): OkHttpClient.Builder
    fun createRawVkApiOkHttpClient(
        @AccountType type: Int,
        config: ProxyConfig?
    ): OkHttpClient.Builder
}