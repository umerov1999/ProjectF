package dev.ragnarok.filegallery.api

import android.annotation.SuppressLint
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.api.HttpLoggerAndParser.serverHeader
import dev.ragnarok.filegallery.kJson
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings
import dev.ragnarok.filegallery.util.UncompressDefaultInterceptor
import dev.ragnarok.filegallery.util.Utils.firstNonEmptyString
import dev.ragnarok.filegallery.util.serializeble.msgpack.MsgPack
import dev.ragnarok.filegallery.util.serializeble.retrofit.kotlinx.serialization.jsonMsgPackConverterFactory
import dev.ragnarok.filegallery.util.serializeble.retrofit.rxjava3.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.core.Single
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class OtherRetrofitProvider @SuppressLint("CheckResult") constructor(private val mainSettings: IMainSettings) :
    IOtherRetrofitProvider {
    private val localServerRetrofitLock = Any()
    private var localServerRetrofitInstance: RetrofitWrapper? = null
    private fun onLocalServerSettingsChanged() {
        synchronized(localServerRetrofitLock) {
            localServerRetrofitInstance?.cleanup()
            localServerRetrofitInstance = null
        }
    }

    private fun createLocalServerRetrofit(): Retrofit {
        val localSettings = mainSettings.getLocalServer()
        val builder = OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request =
                    chain.request().newBuilder().serverHeader(false)
                        .addHeader("User-Agent", Constants.USER_AGENT)
                        .build()
                chain.proceed(request)
            }).addInterceptor(Interceptor { chain: Interceptor.Chain ->
                if (chain.request().body is MultipartBody) {
                    return@Interceptor chain.proceed(chain.request())
                }
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
            })
            .addInterceptor(UncompressDefaultInterceptor)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        val url = firstNonEmptyString(localSettings.url, "https://debug.dev")!!
        return Retrofit.Builder()
            .baseUrl("$url/method/")
            .addConverterFactory(
                KCONVERTER_FACTORY
            )
            .addCallAdapterFactory(RX_ADAPTER_FACTORY)
            .client(builder.build())
            .build()
    }

    override fun provideLocalServerRetrofit(): Single<RetrofitWrapper> {
        return Single.fromCallable {
            if (localServerRetrofitInstance == null) {
                synchronized(localServerRetrofitLock) {
                    if (localServerRetrofitInstance == null) {
                        localServerRetrofitInstance =
                            RetrofitWrapper.wrap(createLocalServerRetrofit())
                    }
                }
            }
            localServerRetrofitInstance!!
        }
    }

    companion object {
        private val KCONVERTER_FACTORY = jsonMsgPackConverterFactory(kJson, MsgPack())
        private val RX_ADAPTER_FACTORY = RxJava3CallAdapterFactory.create()
    }

    init {
        mainSettings.observeLocalServer()
            .subscribe { onLocalServerSettingsChanged() }
    }
}