package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import java.util.Collections

@SuppressLint("CheckResult")
class VKRestProvider(
    private val proxyManager: IProxySettings,
    private val clientFactory: IVKMethodHttpClientFactory
) : IVKRestProvider {
    private val restCacheLock = Any()
    private val serviceRestLock = Any()

    private val restCache = Collections.synchronizedMap(HashMap<Long, SimplePostHttp>(1))

    @Volatile
    private var serviceRest: SimplePostHttp? = null
    private fun onProxySettingsChanged() {
        synchronized(restCacheLock) {
            for ((_, value) in restCache) {
                value?.stop()
            }
            restCache.clear()
        }
    }

    override fun provideNormalRest(accountId: Long): Flow<SimplePostHttp> {
        return flow {
            var rest: SimplePostHttp?
            synchronized(restCacheLock) {
                val tmp = restCache[accountId]
                if (tmp != null) {
                    rest = tmp
                } else {
                    val client = clientFactory.createDefaultVkHttpClient(
                        accountId,
                        proxyManager.activeProxy
                    )
                    rest = createDefaultVkApiRest(client)
                    restCache.put(accountId, rest)
                }
            }
            emit(rest ?: return@flow)
        }
    }

    override fun provideCustomRest(accountId: Long, token: String): Flow<SimplePostHttp> {
        return flow {
            val client = clientFactory.createCustomVkHttpClient(
                accountId,
                token,
                proxyManager.activeProxy
            )
            emit(createDefaultVkApiRest(client))
        }
    }

    override fun provideServiceRest(): Flow<SimplePostHttp> {
        return flow {
            if (serviceRest == null) {
                synchronized(serviceRestLock) {
                    if (serviceRest == null) {
                        val client = clientFactory.createServiceVkHttpClient(
                            proxyManager.activeProxy
                        )
                        serviceRest = createDefaultVkApiRest(client)
                    }
                }
            }
            emit(serviceRest ?: return@flow)
        }
    }

    override fun provideNormalHttpClient(accountId: Long): Flow<OkHttpClient.Builder> {
        return flow {
            emit(
                clientFactory.createDefaultVkHttpClient(
                    accountId,
                    proxyManager.activeProxy
                )
            )
        }
    }

    override fun provideRawHttpClient(
        @AccountType type: Int,
        customDeviceName: String?
    ): Flow<OkHttpClient.Builder> {
        return flow {
            emit(
                clientFactory.createRawVkApiOkHttpClient(
                    type,
                    customDeviceName,
                    proxyManager.activeProxy
                )
            )
        }
    }

    private fun createDefaultVkApiRest(okHttpClient: OkHttpClient.Builder): SimplePostHttp {
        return SimplePostHttp(
            "https://" + Settings.get().main().apiDomain + "/method",
            okHttpClient
        )
    }

    init {
        proxyManager.observeActive
            .sharedFlowToMain { onProxySettingsChanged() }
    }
}
