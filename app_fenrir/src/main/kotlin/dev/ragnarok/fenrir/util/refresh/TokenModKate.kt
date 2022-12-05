package dev.ragnarok.fenrir.util.refresh

import android.os.Build
import android.util.Base64
import dev.ragnarok.fenrir.Constants.KATE_APP_VERSION_CODE
import dev.ragnarok.fenrir.Constants.KATE_APP_VERSION_NAME
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.api.HttpLoggerAndParser
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.ProxyUtil.applyProxyConfig
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.and

object TokenModKate {
    private const val rid = 1
    private var pair: KeyPair? = null
    private fun userAgent(): String {
        return "Android-GCM/1.5 (" + Build.DEVICE +
                ' ' +
                Build.ID +
                ')'
    }

    @Throws(IOException::class)
    private fun receipt(auth: String): String {
        var str: String?
        val str2: String
        val genNewKey = genNewKey()
        val sig = getSig(genNewKey)
        var encoded = pair?.public?.encoded
        try {
            encoded = encoded?.let { MessageDigest.getInstance("SHA1").digest(it) }
            str = null
        } catch (unused: NoSuchAlgorithmException) {
            str = ""
        }
        str2 = if (str == null) {
            encoded?.set(0, ((encoded[0] and 15) + 112 and 255).toByte())
            Base64.encodeToString(encoded, Base64.NO_WRAP).substring(0, 11)
        } else {
            str
        }
        val arrayList = ArrayList<String>()
        fillParams(
            arrayList,
            sig,
            genNewKey,
            str2,
            auth.split(Regex(" ")).toTypedArray()[1].split(Regex(":")).toTypedArray()[0]
        )
        return doRequest("https://android.clients.google.com/c2dm/register3", arrayList, auth)
    }

    fun requestToken(): String? {
        return try {
            println("Token register start")
            val strArr = arrayOf(
                "4537286713832810256:3813922857350986999",
                "4607161437294568617:4436643741345745345",
                "4031819488942003867:1675892049294949499",
                "3665846370517392830:3012248377502379040"
            )
            val str3 = "AidLogin " + strArr[Random().nextInt(strArr.size - 1)]
            val sb3 = receipt(str3)
            if (sb3.contains("REGISTRATION_ERROR")) {
                println("Token register fail")
                return null
            }
            println("Token register OK")
            sb3.split(Regex("\\|ID\\|$rid\\|:")).toTypedArray()[1]
        } catch (unused: Exception) {
            null
        }
    }

    private fun fillParams(
        list: MutableList<String>,
        str: String?,
        str2: String,
        str3: String,
        device: String
    ) {
        list.add("X-scope=GCM")
        list.add("X-X-subscription=54740537194")
        list.add("X-gmp_app_id=1:54740537194:android:fa11238ac5d9b469")
        list.add("X-subtype=54740537194")
        list.add("X-X-subtype=54740537194")
        list.add("X-app_ver=$KATE_APP_VERSION_CODE")
        list.add("X-kid=|ID|$rid|")
        list.add("X-X-kid=|ID|$rid|")
        list.add("X-osv=" + Build.VERSION.SDK_INT)
        list.add("X-sig=$str")
        list.add("X-cliv=iid-12211000")
        list.add("X-gmsv=200313005")
        list.add("X-pub2=$str2")
        list.add("X-appid=$str3")
        list.add("X-subscription=54740537194")
        list.add("X-app_ver_name=$KATE_APP_VERSION_NAME")
        list.add("app=com.perm.kate_new_6")
        list.add("sender=54740537194")
        list.add("device=$device")
        list.add("cert=966882ba564c2619d55d0a9afd4327a38c327456")
        list.add("app_ver=$KATE_APP_VERSION_CODE")
        list.add("info=U_ojcf1ahbQaUO6eTSP7b7WomakK_hY")
        list.add("gcm_ver=200313005")
    }

    private fun join(str: String, iterable: Iterable<String>): String {
        var str2 = StringBuilder()
        for (next in iterable) {
            if (str2.isEmpty()) {
                str2 = StringBuilder(next)
            } else {
                str2.append(str).append(next)
            }
        }
        return str2.toString()
    }

    private fun join(str: String, strArr: Array<String>): String {
        return join(str, listOf(*strArr))
    }

    @Throws(IOException::class)
    private fun doRequest(str: String, list: List<String>, str3: String): String {
        val builder: OkHttpClient.Builder = OkHttpClient.Builder()
            .readTimeout(40, TimeUnit.SECONDS)
            .connectTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                chain.proceed(
                    chain.toRequestBuilder(false)
                        .addHeader("User-Agent", userAgent())
                        .addHeader("Authorization", str3)
                        .addHeader("app", "com.perm.kate_new_6")
                        .addHeader("Gcm-ver", "200313005")
                        .addHeader("Gcm-cert", "966882ba564c2619d55d0a9afd4327a38c327456")
                        .build()
                )
            })
        applyProxyConfig(builder, proxySettings.activeProxy)
        HttpLoggerAndParser.adjust(builder)
        HttpLoggerAndParser.configureToIgnoreCertificates(builder)
        val formBody = FormBody.Builder()
        for (i in list) {
            val v = i.split(Regex("=")).toTypedArray()
            formBody.add(v[0], v[1])
        }
        val request: Request = Request.Builder()
            .url(str)
            .post(formBody.build())
            .build()
        return builder.build().newCall(request).execute().body.string()
    }

    private fun genNewKey(): String {
        try {
            val instance = KeyPairGenerator.getInstance("RSA")
            instance.initialize(2048)
            pair = instance.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return Base64.encodeToString(pair?.public?.encoded, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    private fun getSig(str: String): String? {
        return try {
            val privateKey = (pair ?: return null).private
            val instance =
                Signature.getInstance(if (privateKey is RSAPrivateKey) "SHA256withRSA" else "SHA256withECDSA")
            instance.initSign(privateKey)
            instance.update(
                join("\n", arrayOf("com.perm.kate_new_6", str)).toByteArray(
                    StandardCharsets.UTF_8
                )
            )
            Base64.encodeToString(instance.sign(), Base64.URL_SAFE or Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    init {
        genNewKey()
    }
}