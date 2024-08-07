package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.UserAgentTool
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.vkHeader
import dev.ragnarok.fenrir.api.rest.SimplePostHttp
import dev.ragnarok.fenrir.settings.IProxySettings
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.sharedFlowToMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@SuppressLint("CheckResult")
class UploadRestProvider(private val proxySettings: IProxySettings) : IUploadRestProvider {
    private val uploadRestLock = Any()

    @Volatile
    private var uploadRestInstance: SimplePostHttp? = null
    private fun onProxySettingsChanged() {
        synchronized(uploadRestLock) {
            if (uploadRestInstance != null) {
                uploadRestInstance?.stop()
                uploadRestInstance = null
            }
        }
    }

    override fun provideUploadRest(): Flow<SimplePostHttp> {
        return flow {
            if (uploadRestInstance == null) {
                synchronized(uploadRestLock) {
                    if (uploadRestInstance == null) {
                        uploadRestInstance = createUploadRest()
                    }
                }
            }
            emit(uploadRestInstance ?: return@flow)
        }
    }

    private fun createUploadRest(): SimplePostHttp {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(Constants.UPLOAD_TIMEOUT, TimeUnit.SECONDS)
            .connectTimeout(Constants.UPLOAD_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.UPLOAD_TIMEOUT, TimeUnit.SECONDS)
            .callTimeout(Constants.UPLOAD_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request =
                    chain.toRequestBuilder(false).vkHeader(true).addHeader(
                        "User-Agent", UserAgentTool.USER_AGENT_CURRENT_ACCOUNT
                    ).build()
                chain.proceed(request)
            })
        ProxyUtil.applyProxyConfig(builder, proxySettings.activeProxy)
        HttpLoggerAndParser.adjustUpload(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        return SimplePostHttp(
            "https://" + Settings.get().main().apiDomain + "/method",
            builder
        )
    }

    init {
        proxySettings.observeActive
            .sharedFlowToMain { onProxySettingsChanged() }
    }
}
