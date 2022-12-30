package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Constants.USER_AGENT
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import dev.ragnarok.fenrir.nonNullNoEmpty
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.UncompressDefaultInterceptor
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OtherVkRestProvider @SuppressLint("CheckResult") constructor(private val proxySettings: IProxySettings) :
    IOtherVkRestProvider {
    private val longpollRestLock = Any()
    private val localServerRestLock = Any()
    private var longpollRestInstance: SimplePostHttp? = null
    private var localServerRestInstance: SimplePostHttp? = null
    private fun onProxySettingsChanged() {
        synchronized(longpollRestLock) {
            if (longpollRestInstance != null) {
                longpollRestInstance?.stop()
                longpollRestInstance = null
            }
        }
        synchronized(localServerRestLock) {
            if (localServerRestInstance != null) {
                localServerRestInstance?.stop()
                localServerRestInstance = null
            }
        }
    }

    override fun provideAuthRest(): Single<SimplePostHttp> {
        return Single.fromCallable {
            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
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
            SimplePostHttp("https://" + Settings.get().other().get_Auth_Domain(), builder)
        }
    }

    override fun provideAuthServiceRest(): Single<SimplePostHttp> {
        return Single.fromCallable {
            val builder: OkHttpClient.Builder = OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.SECONDS)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .callTimeout(15, TimeUnit.SECONDS)
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
            SimplePostHttp(
                "https://" + Settings.get().other().get_Api_Domain() + "/method",
                builder
            )
        }
    }

    private fun createLocalServerRest(): SimplePostHttp {
        val localSettings = Settings.get().other().localServer
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
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
                    .post(formBuilder.build())
                    .build()
                chain.proceed(request)
            }).addInterceptor(UncompressDefaultInterceptor)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        val url = Utils.firstNonEmptyString(localSettings.url, "https://debug.dev")!!
        return SimplePostHttp("$url/method", builder)
    }

    private fun createLongpollRestInstance(): SimplePostHttp {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(25, TimeUnit.SECONDS)
            .connectTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
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
        return SimplePostHttp(
            "https://" + Settings.get().other().get_Api_Domain() + "/method",
            builder
        )
    }

    override fun provideLocalServerRest(): Single<SimplePostHttp> {
        return Single.fromCallable {
            if (localServerRestInstance == null) {
                synchronized(localServerRestLock) {
                    if (localServerRestInstance == null) {
                        localServerRestInstance = createLocalServerRest()
                    }
                }
            }
            localServerRestInstance!!
        }
    }

    override fun provideLongpollRest(): Single<SimplePostHttp> {
        return Single.fromCallable {
            if (longpollRestInstance == null) {
                synchronized(longpollRestLock) {
                    if (longpollRestInstance == null) {
                        longpollRestInstance = createLongpollRestInstance()
                    }
                }
            }
            longpollRestInstance!!
        }
    }

    init {
        proxySettings.observeActive()
            .subscribe { onProxySettingsChanged() }
    }
}
