package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Constants.USER_AGENT
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.api.RetrofitWrapper.Companion.wrap
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.UncompressDefaultInterceptor
import dev.ragnarok.fenrir.util.Utils
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import dev.ragnarok.fenrir.util.serializeble.retrofit.kotlinx.serialization.jsonMsgPackConverterFactory
import dev.ragnarok.fenrir.util.serializeble.retrofit.rxjava3.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.core.Single
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class OtherVkRetrofitProvider @SuppressLint("CheckResult") constructor(private val proxySettings: IProxySettings) :
    IOtherVkRetrofitProvider {
    private val longpollRetrofitLock = Any()
    private val localServerRetrofitLock = Any()
    private var longpollRetrofitInstance: RetrofitWrapper? = null
    private var localServerRetrofitInstance: RetrofitWrapper? = null
    private fun onProxySettingsChanged() {
        synchronized(longpollRetrofitLock) {
            if (longpollRetrofitInstance != null) {
                longpollRetrofitInstance?.cleanup()
                longpollRetrofitInstance = null
            }
        }
        synchronized(localServerRetrofitLock) {
            if (localServerRetrofitInstance != null) {
                localServerRetrofitInstance?.cleanup()
                localServerRetrofitInstance = null
            }
        }
    }

    override fun provideAuthRetrofit(): Single<RetrofitWrapper> {
        return Single.fromCallable {
            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                    val request =
                        chain.toRequestBuilder(false).vkHeader(true)
                            .addHeader(
                                "User-Agent", USER_AGENT(
                                    Constants.DEFAULT_ACCOUNT_TYPE
                                )
                            ).build()
                    chain.proceed(request)
                })
                .addInterceptor(UncompressDefaultInterceptor)
            ProxyUtil.applyProxyConfig(builder, proxySettings.activeProxy)
            HttpLoggerAndParser.adjust(builder)
            HttpLoggerAndParser.configureToIgnoreCertificates(builder)
            val retrofit = Retrofit.Builder()
                .baseUrl("https://" + Settings.get().other().get_Auth_Domain() + "/")
                .addConverterFactory(KCONVERTER_FACTORY)
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .client(builder.build())
                .build()
            wrap(retrofit, false)
        }
    }

    override fun provideAuthServiceRetrofit(): Single<RetrofitWrapper> {
        return Single.fromCallable {
            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                    val request =
                        chain.toRequestBuilder(false).vkHeader(true)
                            .addHeader(
                                "User-Agent", USER_AGENT(
                                    AccountType.BY_TYPE
                                )
                            ).build()
                    chain.proceed(request)
                })
                .addInterceptor(UncompressDefaultInterceptor)
            ProxyUtil.applyProxyConfig(builder, proxySettings.activeProxy)
            HttpLoggerAndParser.adjust(builder)
            HttpLoggerAndParser.configureToIgnoreCertificates(builder)
            val retrofit = Retrofit.Builder()
                .baseUrl("https://" + Settings.get().other().get_Api_Domain() + "/method/")
                .addConverterFactory(KCONVERTER_FACTORY)
                .addCallAdapterFactory(RX_ADAPTER_FACTORY)
                .client(builder.build())
                .build()
            wrap(retrofit, false)
        }
    }

    private fun createLocalServerRetrofit(): Retrofit {
        val localSettings = Settings.get().other().localServer
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request =
                    chain.toRequestBuilder(false).vkHeader(false).addHeader(
                        "User-Agent", USER_AGENT(
                            AccountType.BY_TYPE
                        )
                    ).build()
                chain.proceed(request)
            }).addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val original = chain.request()
                val formBuilder = FormBody.Builder()
                val body = original.body
                if (body is FormBody) {
                    for (i in 0 until body.size) {
                        formBuilder.add(body.name(i), body.value(i))
                    }
                }
                localSettings.password.nonNullNoEmpty {
                    formBuilder.add("password", it)
                }
                val request = original.newBuilder()
                    .method("POST", formBuilder.build())
                    .build()
                chain.proceed(request)
            }).addInterceptor(UncompressDefaultInterceptor)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        val url = Utils.firstNonEmptyString(localSettings.url, "https://debug.dev")!!
        return Retrofit.Builder()
            .baseUrl("$url/method/")
            .addConverterFactory(
                KCONVERTER_FACTORY
            )
            .addCallAdapterFactory(RX_ADAPTER_FACTORY)
            .client(builder.build())
            .build()
    }

    private fun createLongpollRetrofitInstance(): Retrofit {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request =
                    chain.toRequestBuilder(false).vkHeader(true).addHeader(
                        "User-Agent", USER_AGENT(
                            AccountType.BY_TYPE
                        )
                    ).build()
                chain.proceed(request)
            })
            .addInterceptor(UncompressDefaultInterceptor)
        ProxyUtil.applyProxyConfig(builder, proxySettings.activeProxy)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        return Retrofit.Builder()
            .baseUrl("https://" + Settings.get().other().get_Api_Domain() + "/method/") // dummy
            .addConverterFactory(KCONVERTER_FACTORY)
            .addCallAdapterFactory(RX_ADAPTER_FACTORY)
            .client(builder.build())
            .build()
    }

    override fun provideLocalServerRetrofit(): Single<RetrofitWrapper> {
        return Single.fromCallable {
            if (localServerRetrofitInstance == null) {
                synchronized(localServerRetrofitLock) {
                    if (localServerRetrofitInstance == null) {
                        localServerRetrofitInstance = wrap(createLocalServerRetrofit())
                    }
                }
            }
            localServerRetrofitInstance!!
        }
    }

    override fun provideLongpollRetrofit(): Single<RetrofitWrapper> {
        return Single.fromCallable {
            if (longpollRetrofitInstance == null) {
                synchronized(longpollRetrofitLock) {
                    if (longpollRetrofitInstance == null) {
                        longpollRetrofitInstance = wrap(createLongpollRetrofitInstance())
                    }
                }
            }
            longpollRetrofitInstance!!
        }
    }

    companion object {
        private val KCONVERTER_FACTORY = jsonMsgPackConverterFactory(kJson, MsgPack())
        private val RX_ADAPTER_FACTORY = RxJava3CallAdapterFactory.create()
    }

    init {
        proxySettings.observeActive()
            .subscribe { onProxySettingsChanged() }
    }
}
