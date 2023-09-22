package dev.ragnarok.fenrir.api

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.api.model.Params
import dev.ragnarok.fenrir.model.ParserType
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.OkHttp3LoggingInterceptor
import dev.ragnarok.fenrir.util.Utils
import okhttp3.*
import okio.*
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object HttpLoggerAndParser {
    /*
    fun selectConverterFactory(
        json: Converter.Factory,
        msgpack: Converter.Factory
    ): Converter.Factory {
        return if (Settings.get().main().currentParser == ParserType.MSGPACK) {
            msgpack
        } else {
            json
        }
    }
     */

    abstract class GzipFormBody(val original: List<Params>) : RequestBody()

    fun FormBody.gzipFormBody(): GzipFormBody {
        val o = ArrayList<Params>(size)
        for (i in 0 until size) {
            val tmp = Params()
            tmp.key = name(i)
            tmp.value = value(i)
            o.add(tmp)
        }
        return object : GzipFormBody(o) {
            override fun contentType(): MediaType {
                return this@gzipFormBody.contentType()
            }

            override fun contentLength(): Long {
                val g = Buffer()
                val f = GzipSink(g).buffer()
                this@gzipFormBody.writeTo(f)
                f.close()
                return g.size
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                val buf = sink.gzip().buffer()
                this@gzipFormBody.writeTo(buf)
                buf.close()
            }

            override fun isOneShot(): Boolean {
                return this@gzipFormBody.isOneShot()
            }
        }
    }

    fun Interceptor.Chain.toRequestBuilder(supportCompressGzip: Boolean): Request.Builder {
        val request = request()
        val o = request.newBuilder()
        if (supportCompressGzip && request.body is FormBody && Utils.isCompressOutgoingTraffic) {
            (request.body as FormBody).gzipFormBody().let {
                o.addHeader("Content-Encoding", "gzip")
                o.post(it)
            }
        }
        return o
    }

    fun Request.Builder.makeVK(supportCompressGzip: Boolean): Request.Builder {
        val request = build()
        if (supportCompressGzip && request.body is FormBody && Utils.isCompressOutgoingTraffic) {
            (request.body as FormBody).gzipFormBody().let {
                addHeader("Content-Encoding", "gzip")
                post(it)
            }
        }
        return this
    }

    @Suppress("unused_parameter")
    fun Request.Builder.vkHeader(onlyJson: Boolean): Request.Builder {
        addHeader("X-VK-Android-Client", "new")
        if (/*!onlyJson && */Utils.currentParser == ParserType.MSGPACK) {
            addHeader("X-Response-Format", "msgpack")
        }
        return this
    }

    private val DEFAULT_LOGGING_INTERCEPTOR: OkHttp3LoggingInterceptor by lazy {
        OkHttp3LoggingInterceptor().setLevel(OkHttp3LoggingInterceptor.Level.BODY)
    }

    private val UPLOAD_LOGGING_INTERCEPTOR: OkHttp3LoggingInterceptor by lazy {
        //OkHttp3LoggingInterceptor().setLevel(OkHttp3LoggingInterceptor.Level.HEADERS)
        OkHttp3LoggingInterceptor().setLevel(OkHttp3LoggingInterceptor.Level.BODY)
    }

    fun adjust(builder: OkHttpClient.Builder) {
        if (Constants.IS_DEBUG) {
            /*
            if (Settings.get().main().currentParser == ParserType.JSON) {
                builder.addInterceptor(DEFAULT_LOGGING_INTERCEPTOR)
            } else {
                builder.addInterceptor(UPLOAD_LOGGING_INTERCEPTOR)
            }
             */
            builder.addInterceptor(DEFAULT_LOGGING_INTERCEPTOR)
        }
    }

    fun adjustUpload(builder: OkHttpClient.Builder) {
        if (Constants.IS_DEBUG) {
            builder.addInterceptor(UPLOAD_LOGGING_INTERCEPTOR)
        }
    }

    fun configureToIgnoreCertificates(builder: OkHttpClient.Builder) {
        if (Settings.get().main().isValidate_tls) {
            return
        }
        try {
            val trustAllCerts: Array<TrustManager> = arrayOf(
                @SuppressLint("CustomX509TrustManager")
                object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String?
                    ) {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String?
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                }
            )
            val sslContext: SSLContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
