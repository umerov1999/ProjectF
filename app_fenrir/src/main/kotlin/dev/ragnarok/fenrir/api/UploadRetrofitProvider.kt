package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.AccountType
import dev.ragnarok.fenrir.Constants.USER_AGENT
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.api.RetrofitWrapper.Companion.wrap
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.serializeble.msgpack.MsgPack
import dev.ragnarok.fenrir.util.serializeble.retrofit.kotlinx.serialization.jsonMsgPackConverterFactory
import dev.ragnarok.fenrir.util.serializeble.retrofit.rxjava3.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.core.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@SuppressLint("CheckResult")
class UploadRetrofitProvider(private val proxySettings: IProxySettings) : IUploadRetrofitProvider {
    private val uploadRetrofitLock = Any()

    @Volatile
    private var uploadRetrofitInstance: RetrofitWrapper? = null
    private fun onProxySettingsChanged() {
        synchronized(uploadRetrofitLock) {
            if (uploadRetrofitInstance != null) {
                uploadRetrofitInstance?.cleanup()
                uploadRetrofitInstance = null
            }
        }
    }

    override fun provideUploadRetrofit(): Single<RetrofitWrapper> {
        return Single.fromCallable {
            if (uploadRetrofitInstance == null) {
                synchronized(uploadRetrofitLock) {
                    if (uploadRetrofitInstance == null) {
                        uploadRetrofitInstance = wrap(createUploadRetrofit(), true)
                    }
                }
            }
            uploadRetrofitInstance!!
        }
    }

    private fun createUploadRetrofit(): Retrofit {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request =
                    chain.toRequestBuilder(false).vkHeader(true).addHeader(
                        "User-Agent", USER_AGENT(
                            AccountType.BY_TYPE
                        )
                    ).build()
                chain.proceed(request)
            })
        ProxyUtil.applyProxyConfig(builder, proxySettings.activeProxy)
        HttpLoggerAndParser.adjustUpload(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        return Retrofit.Builder()
            .baseUrl("https://" + Settings.get().other().get_Api_Domain() + "/method/") // dummy
            .addConverterFactory(KCONVERTER_FACTORY)
            .addCallAdapterFactory(RX_ADAPTER_FACTORY)
            .client(builder.build())
            .build()
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
