package dev.ragnarok.fenrir.util.refresh

import android.os.Build
import android.util.Base64
import dev.ragnarok.fenrir.Constants.VK_ANDROID_APP_VERSION_CODE
import dev.ragnarok.fenrir.Constants.VK_ANDROID_APP_VERSION_NAME
import dev.ragnarok.fenrir.Includes.proxySettings
import dev.ragnarok.fenrir.api.HttpLoggerAndParser
import dev.ragnarok.fenrir.api.HttpLoggerAndParser.toRequestBuilder
import dev.ragnarok.fenrir.api.ProxyUtil.applyProxyConfig
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.experimental.and

object TokenModOfficialVK {
    private const val rid = 1
    private var pair: KeyPair? = null
    private fun userAgent(): String {
        return "Android-GCM/1.5 (" + Build.DEVICE +
                ' ' +
                Build.ID +
                ')'
    }

    fun getNonce(timestamp: Long): String? {
        val valueOf = timestamp.toString()
        val byteArrayOutputStream = ByteArrayOutputStream()
        val bArr = ByteArray(24)
        Random().nextBytes(bArr)
        return try {
            byteArrayOutputStream.write(bArr)
            byteArrayOutputStream.write(valueOf.toByteArray())
            Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)
        } catch (unused: IOException) {
            null
        }
    }

    @Throws(IOException::class)
    private fun receipt(auth: String, clear: Boolean): String {
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
            auth.split(Regex(" ")).toTypedArray()[1].split(Regex(":")).toTypedArray()[0],
            clear
        )
        return doRequest("https://android.clients.google.com/c2dm/register3", arrayList, auth)
    }

    fun requestToken(): List<String>? {
        val ret: MutableList<String> = ArrayList(2)
        return try {
            println("Token register start")
            val strArr = arrayOf(
                "4537286713832810256:3813922857350986999",
                "4607161437294568617:4436643741345745345",
                "4031819488942003867:1675892049294949499",
                "3665846370517392830:3012248377502379040"
            )
            val str3 = "AidLogin " + strArr[Random().nextInt(strArr.size - 1)]
            var sb3 = receipt(str3, false)
            if (sb3.contains("REGISTRATION_ERROR")) {
                println("Token register fail")
                return null
            }
            ret.add(sb3.split(Regex("\\|ID\\|$rid\\|:")).toTypedArray()[1])
            sb3 = receipt(str3, true)
            if (sb3.contains("REGISTRATION_ERROR")) {
                println("Token register fail")
                return null
            }
            ret.add(sb3.split(Regex("\\|ID\\|$rid\\|:")).toTypedArray()[1])
            println("Token register OK")
            ret
        } catch (unused: Exception) {
            null
        }
    }

    private fun fillParams(
        list: MutableList<String>,
        str: String?,
        str2: String,
        str3: String,
        device: String,
        clear: Boolean
    ) {
        if (clear) {
            list.add("X-scope=GCM")
            list.add("X-delete=1")
            list.add("X-X-delete=1")
        } else {
            list.add("X-scope=*")
            list.add("X-X-subscription=841415684880")
            list.add("X-gmp_app_id=1:841415684880:android:632f429381141121")
        }
        list.add("X-subtype=841415684880")
        list.add("X-X-subtype=841415684880")
        list.add("X-app_ver=$VK_ANDROID_APP_VERSION_CODE")
        list.add("X-kid=|ID|$rid|")
        list.add("X-X-kid=|ID|$rid|")
        list.add("X-osv=" + Build.VERSION.SDK_INT)
        list.add("X-sig=$str")
        list.add("X-cliv=fiid-9877000")
        list.add("X-gmsv=200313005")
        list.add("X-pub2=$str2")
        list.add("X-appid=$str3")
        list.add("X-subscription=841415684880")
        list.add("X-app_ver_name=$VK_ANDROID_APP_VERSION_NAME")
        list.add("app=com.vkontakte.android")
        list.add("sender=841415684880")
        list.add("device=$device")
        list.add("cert=48761eef50ee53afc4cc9c5f10e6bde7f8f5b82f")
        list.add("app_ver=$VK_ANDROID_APP_VERSION_CODE")
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
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                chain.proceed(
                    chain.toRequestBuilder(false)
                        .addHeader("User-Agent", userAgent())
                        .addHeader("Authorization", str3)
                        .addHeader("app", "com.vkontakte.android")
                        .addHeader("Gcm-ver", "200313005")
                        .addHeader("Gcm-cert", "48761eef50ee53afc4cc9c5f10e6bde7f8f5b82f")
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
                join("\n", arrayOf("com.vkontakte.android", str)).toByteArray(
                    Charsets.UTF_8
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