package dev.ragnarok.fenrir.api

import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.BuildConfig
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.model.ProxyConfig
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.UncompressDefaultInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class VKMethodHttpClientFactory : IVKMethodHttpClientFactory {
    override fun createDefaultVkHttpClient(
        accountId: Long,
        config: ProxyConfig?
    ): OkHttpClient.Builder {
        return createDefaultVkApiOkHttpClient(
            DefaultVKApiInterceptor(
                accountId,
                Constants.API_VERSION
            ), config
        )
    }

    override fun createCustomVkHttpClient(
        accountId: Long,
        token: String,
        config: ProxyConfig?
    ): OkHttpClient.Builder {
        return createDefaultVkApiOkHttpClient(
            CustomTokenVKApiInterceptor(
                token,
                Constants.API_VERSION,
                Settings.get().accounts().getType(accountId),
                Settings.get().accounts().getDevice(accountId),
                accountId
            ), config
        )
    }

    override fun createServiceVkHttpClient(config: ProxyConfig?): OkHttpClient.Builder {
        return createDefaultVkApiOkHttpClient(
            CustomTokenVKApiInterceptor(
                BuildConfig.SERVICE_TOKEN,
                Constants.API_VERSION,
                Constants.DEFAULT_ACCOUNT_TYPE,
                null,
                null
            ), config
        )
    }

    private fun createDefaultVkApiOkHttpClient(
        interceptor: AbsVKApiInterceptor,
        config: ProxyConfig?
    ): OkHttpClient.Builder {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .readTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(UncompressDefaultInterceptor)
        ProxyUtil.applyProxyConfig(builder, config)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        return builder
    }

    override fun createRawVkApiOkHttpClient(
        @AccountType type: Int,
        customDeviceName: String?,
        config: ProxyConfig?
    ): OkHttpClient.Builder {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request = chain.toRequestBuilder(true).vkHeader(false)
                    .addHeader(
                        "User-Agent",
                        UserAgentTool.getAccountUserAgent(type, customDeviceName)
                    ).build()
                chain.proceed(request)
            }).addInterceptor(UncompressDefaultInterceptor)
        ProxyUtil.applyProxyConfig(builder, config)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        return builder
    }
}
