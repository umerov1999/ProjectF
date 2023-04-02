package dev.ragnarok.filegallery.api

import android.annotation.SuppressLint
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.api.HttpLoggerAndParser.serverHeader
import dev.ragnarok.filegallery.api.rest.SimplePostHttp
import dev.ragnarok.filegallery.nonNullNoEmpty
import dev.ragnarok.filegallery.settings.ISettings.IMainSettings
import dev.ragnarok.filegallery.util.UncompressDefaultInterceptor
import dev.ragnarok.filegallery.util.Utils.firstNonEmptyString
import io.reactivex.rxjava3.core.Single
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class LocalServerRestProvider @SuppressLint("CheckResult") constructor(private val mainSettings: IMainSettings) :
    ILocalServerRestProvider {
    private val localServerRestLock = Any()
    private var localServerRestInstance: SimplePostHttp? = null
    private fun onLocalServerSettingsChanged() {
        synchronized(localServerRestLock) {
            localServerRestInstance?.stop()
            localServerRestInstance = null
        }
    }

    private fun createLocalServerRest(): SimplePostHttp {
        val localSettings = mainSettings.getLocalServer()
        val builder = OkHttpClient.Builder()
            .readTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(Constants.API_TIMEOUT, TimeUnit.SECONDS)
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
                    .post(formBuilder.build())
                    .build()
                chain.proceed(request)
            })
            .addInterceptor(UncompressDefaultInterceptor)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        val url = firstNonEmptyString(localSettings.url, "https://debug.dev")!!
        return SimplePostHttp("$url/method", builder)
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

    init {
        mainSettings.observeLocalServer()
            .subscribe { onLocalServerSettingsChanged() }
    }
}