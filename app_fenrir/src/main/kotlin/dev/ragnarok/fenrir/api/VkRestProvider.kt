package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import io.reactivex.rxjava3.core.Single
import okhttp3.OkHttpClient
import java.util.*

@SuppressLint("CheckResult")
class VkRestProvider(
    private val proxyManager: IProxySettings,
    private val clientFactory: IVkMethodHttpClientFactory
) : IVkRestProvider {
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

    override fun provideNormalRest(accountId: Long): Single<SimplePostHttp> {
        return Single.fromCallable {
            var rest: SimplePostHttp?
            synchronized(restCacheLock) {
                restCache[accountId]?.let {
                    return@fromCallable it
                }
                val client = clientFactory.createDefaultVkHttpClient(
                    accountId,
                    proxyManager.activeProxy
                )
                rest = createDefaultVkApiRest(client)
                restCache.put(accountId, rest)
            }
            rest!!
        }
    }

    override fun provideCustomRest(accountId: Long, token: String): Single<SimplePostHttp> {
        return Single.fromCallable {
            val client = clientFactory.createCustomVkHttpClient(
                accountId,
                token,
                proxyManager.activeProxy
            )
            createDefaultVkApiRest(client)
        }
    }

    override fun provideServiceRest(): Single<SimplePostHttp> {
        return Single.fromCallable {
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
            serviceRest!!
        }
    }

    override fun provideNormalHttpClient(accountId: Long): Single<OkHttpClient.Builder> {
        return Single.fromCallable {
            clientFactory.createDefaultVkHttpClient(
                accountId,
                proxyManager.activeProxy
            )
        }
    }

    override fun provideRawHttpClient(@AccountType type: Int): Single<OkHttpClient.Builder> {
        return Single.fromCallable {
            clientFactory.createRawVkApiOkHttpClient(
                type,
                proxyManager.activeProxy
            )
        }
    }

    private fun createDefaultVkApiRest(okHttpClient: OkHttpClient.Builder): SimplePostHttp {
        return SimplePostHttp(
            "https://" + Settings.get().other().get_Api_Domain() + "/method",
            okHttpClient
        )
    }

    init {
        proxyManager.observeActive()
            .subscribe { onProxySettingsChanged() }
    }
}
