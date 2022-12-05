package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.api.RetrofitWrapper.Companion.wrap
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import dev.ragnarok.fenrir.util.serializeble.retrofit.kotlinx.serialization.jsonMsgPackConverterFactory
import dev.ragnarok.fenrir.util.serializeble.retrofit.rxjava3.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.core.Single
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.*

@SuppressLint("CheckResult")
class VkRetrofitProvider(
    private val proxyManager: IProxySettings,
    private val clientFactory: IVkMethodHttpClientFactory
) : IVkRetrofitProvider {
    private val retrofitCacheLock = Any()
    private val serviceRetrofitLock = Any()

    private val retrofitCache = Collections.synchronizedMap(HashMap<Int, RetrofitWrapper>(1))

    @Volatile
    private var serviceRetrofit: RetrofitWrapper? = null
    private fun onProxySettingsChanged() {
        synchronized(retrofitCacheLock) {
            for ((_, value) in retrofitCache) {
                value?.cleanup()
            }
            retrofitCache.clear()
        }
    }

    override fun provideNormalRetrofit(accountId: Int): Single<RetrofitWrapper> {
        return Single.fromCallable {
            var retrofit: RetrofitWrapper?
            synchronized(retrofitCacheLock) {
                retrofitCache[accountId]?.let {
                    return@fromCallable it
                }
                val client = clientFactory.createDefaultVkHttpClient(
                    accountId,
                    proxyManager.activeProxy
                )
                retrofit = createDefaultVkApiRetrofit(client)
                retrofitCache.put(accountId, retrofit)
            }
            retrofit!!
        }
    }

    override fun provideCustomRetrofit(accountId: Int, token: String): Single<RetrofitWrapper> {
        return Single.fromCallable {
            val client = clientFactory.createCustomVkHttpClient(
                accountId,
                token,
                proxyManager.activeProxy
            )
            createDefaultVkApiRetrofit(client)
        }
    }

    override fun provideServiceRetrofit(): Single<RetrofitWrapper> {
        return Single.fromCallable {
            if (serviceRetrofit == null) {
                synchronized(serviceRetrofitLock) {
                    if (serviceRetrofit == null) {
                        val client = clientFactory.createServiceVkHttpClient(
                            proxyManager.activeProxy
                        )
                        serviceRetrofit = createDefaultVkApiRetrofit(client)
                    }
                }
            }
            serviceRetrofit!!
        }
    }

    override fun provideNormalHttpClient(accountId: Int): Single<OkHttpClient> {
        return Single.fromCallable {
            clientFactory.createDefaultVkHttpClient(
                accountId,
                proxyManager.activeProxy
            )
        }
    }

    override fun provideRawHttpClient(@AccountType type: Int): Single<OkHttpClient> {
        return Single.fromCallable {
            clientFactory.createRawVkApiOkHttpClient(
                type,
                proxyManager.activeProxy
            )
        }
    }

    private fun createDefaultVkApiRetrofit(okHttpClient: OkHttpClient): RetrofitWrapper {
        return wrap(
            Retrofit.Builder()
                .baseUrl("https://" + Settings.get().other().get_Api_Domain() + "/method/")
                .addConverterFactory(KCONVERTER_FACTORY)
                .addCallAdapterFactory(RX_ADAPTER_FACTORY)
                .client(okHttpClient)
                .build()
        )
    }

    companion object {
        private val KCONVERTER_FACTORY = jsonMsgPackConverterFactory(kJson, MsgPack())
        private val RX_ADAPTER_FACTORY = RxJava3CallAdapterFactory.create()
    }

    init {
        proxyManager.observeActive()
            .subscribe { onProxySettingsChanged() }
    }
}
