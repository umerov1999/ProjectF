package dev.ragnarok.fenrir.util.hls

import com.google.common.io.ByteStreams
import dev.ragnarok.fenrir.util.Utils
import io.reactivex.rxjava3.core.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.URL
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.Key
import java.security.NoSuchAlgorithmException
import java.util.AbstractMap
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class M3U8 {
    private val url: String
    private val output: OutputStream?

    constructor(url: String, filename: String) {
        try {
            this.url = url
            val file = File(filename).absoluteFile
            file.parentFile?.mkdirs()
            output = FileOutputStream(file)
        } catch (ex: FileNotFoundException) {
            throw RuntimeException(ex)
        }
    }

    constructor(url: String) {
        this.url = url
        output = null
    }

    val length: Single<Long>
        get() = Single.create { pp ->
            var ret = 0L
            val client = Utils.createOkHttp(15, false).build()
            try {
                val mediaURL: URL
                var m3u8Url = URL(url)
                getStream(client, m3u8Url).use { im ->
                    if (im == null) {
                        pp.onSuccess(0L)
                        return@create
                    }
                    BufferedReader(InputStreamReader(im)).use { br ->
                        var line: String?
                        val urls = ArrayList<Map.Entry<Long, URL>>()
                        var newurl: Long = 0
                        while (br.readLine().also { line = it } != null) {
                            line = line?.trim { it <= ' ' }
                            line ?: continue
                            val property = checkProperty(line ?: return@create)
                            newurl = if (property == null) {
                                0
                            } else if (property.type == "EXT-X-STREAM-INF") {
                                if (property.properties?.get("BANDWIDTH") != null) (property.properties?.get(
                                    "BANDWIDTH"
                                )?.toLong() ?: 1)
                                else 1
                            } else if (property.type == "FILE" && newurl > 0) {
                                urls.add(AbstractMap.SimpleEntry(newurl, URL(m3u8Url, line)))
                                0
                            } else {
                                0
                            }
                        }
                        urls.sortWith { (key), (key1) ->
                            -key.compareTo(key1)
                        }
                        if (urls.size > 0) {
                            m3u8Url = urls[0].value
                        }
                    }
                }
                mediaURL = m3u8Url
                val list = ArrayList<TSDownload>()
                val iis = getStream(client, mediaURL)
                if (iis == null) {
                    pp.onSuccess(0L)
                    return@create
                }
                BufferedReader(InputStreamReader(iis)).use { br ->
                    val type = KeyType.NONE
                    val key = ByteArray(16)
                    val iv = ByteArray(16)
                    var line: String?
                    while (br.readLine().also { line = it } != null) {
                        line = line?.trim { it <= ' ' }
                        line ?: continue
                        val property = checkProperty(line ?: return@use)
                        if (property != null) {
                            if (property.type == "FILE") {
                                val tsUrl = URL(mediaURL, line)
                                list.add(TSDownload(tsUrl, type, key, iv))
                                for (i in iv.size downTo 1) {
                                    iv[i - 1] = (iv[i - 1] + 1).toByte()
                                    if (iv[i - 1] != 0.toByte()) break
                                }
                            }
                        }
                    }
                }
                for (i in list.indices) {
                    var j = i
                    while (j < list.size && j < i + 1) {
                        val response = client.newCall(
                            Request.Builder()
                                .url(list[j].url)
                                .build()
                        ).execute()
                        ret += if (response.isSuccessful) {
                            val v = response.header("Content-Length")
                            response.body.close()
                            response.close()
                            if (v.isNullOrEmpty()) {
                                pp.onSuccess(0L)
                                return@create
                            }
                            v.toLong()
                        } else {
                            pp.onSuccess(0L)
                            response.close()
                            return@create
                        }
                        j++
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                pp.onSuccess(0L)
                return@create
            }
            val vtt = ret / 188L
            pp.onSuccess(ret - vtt * 4)
        }

    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VALUE")
    fun run(): Boolean {
        val client = Utils.createOkHttp(60, false).build()
        try {
            val mediaURL: URL
            var m3u8Url = URL(url)
            getStream(client, m3u8Url).use { ll ->
                if (ll == null) {
                    return false
                }
                BufferedReader(InputStreamReader(ll)).use { br ->
                    var line: String?
                    val urls = ArrayList<Map.Entry<Long, URL>>()
                    var newurl: Long = 0
                    do {
                        line = br.readLine()?.trim { it <= ' ' }
                        line ?: continue
                        val property = checkProperty(line)
                        newurl = if (property == null) {
                            0
                        } else if (property.type == "EXT-X-STREAM-INF") {
                            if (property.properties?.get("BANDWIDTH") != null) property.properties?.get(
                                "BANDWIDTH"
                            )?.toLong() ?: 1 else 1
                        } else if (property.type == "FILE" && newurl > 0) {
                            urls.add(AbstractMap.SimpleEntry(newurl, URL(m3u8Url, line)))
                            0
                        } else {
                            0
                        }
                    } while (line != null)
                    urls.sortWith { (key), (key1) ->
                        -key.compareTo(key1)
                    }
                    if (urls.size > 0) {
                        m3u8Url = urls[0].value
                    }
                }
            }
            mediaURL = m3u8Url
            val list = ArrayList<TSDownload>()
            val iis = getStream(client, mediaURL) ?: return false
            BufferedReader(InputStreamReader(iis)).use { br ->
                var type: KeyType = KeyType.NONE
                val key = ByteArray(16)
                val iv = ByteArray(16)
                var line: String?
                do {
                    line = br.readLine()?.trim { it <= ' ' }
                    line ?: continue
                    val property = checkProperty(line ?: return@use)
                    if (property != null) {
                        if (property.type == "EXT-X-KEY") {
                            if ("NONE" == property.properties?.get("METHOD")) {
                                type = KeyType.NONE
                                continue
                            }
                            type = KeyType.AES128
                            val keyUrl = URL(mediaURL, property.properties?.get("URI"))
                            val ooo = getStream(client, keyUrl) ?: return false
                            ooo.use { ks ->
                                val keyLen = ks.read(key)
                                if (keyLen != key.size) {
                                    throw RuntimeException("key error")
                                }
                            }
                            if (property.properties?.get("IV") != null) {
                                var ivstr = property.properties?.get("IV")
                                ivstr = ivstr?.substring(2)
                                for (i in iv.indices) {
                                    iv[i] =
                                        ivstr?.substring(i * 2, (i + 1) * 2)?.toInt(16)?.toByte()
                                            ?: 0
                                }
                            } else {
                                Arrays.fill(iv, 0.toByte())
                            }
                        } else if (property.type == "FILE") {
                            val tsUrl = URL(mediaURL, line)
                            list.add(TSDownload(tsUrl, type, key, iv))
                            for (i in iv.size downTo 1) {
                                iv[i - 1] = (iv[i - 1] + 1).toByte()
                                if (iv[i - 1] != 0.toByte()) break
                            }
                        }
                    }
                } while (line != null)
            }
            output.use { fileStream ->
                for (i in list.indices) {
                    var j = i
                    while (j < list.size && j < i + 1) {
                        if (list[j].start(client)) {
                            fileStream?.write(list[j].data)
                        } else {
                            return false
                        }
                        j++
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
        return true
    }

    private enum class KeyType {
        NONE, AES128
    }

    private class TSDownload(val url: URL, val type: KeyType, key: ByteArray, iv: ByteArray) {
        val key: ByteArray = key.clone()
        val iv: ByteArray = iv.clone()
        lateinit var data: ByteArray
            private set

        fun start(client: OkHttpClient): Boolean {
            try {
                val iis = getStream(client, url) ?: return false
                val type = type
                val bytes: ByteArray
                when (type) {
                    KeyType.AES128 -> {
                        val stream = CipherInputStream(iis, getAesCp(key, iv))
                        bytes = ByteStreams.toByteArray(stream)
                        closeQuietly(stream)
                    }

                    KeyType.NONE -> {
                        bytes = ByteStreams.toByteArray(iis)
                    }
                }
                check(bytes.size % TS_PACKET_SIZE == 0) { "MPEG2 TS Files that are not" }
                var i = 0
                while (i < bytes.size) {
                    check(bytes[i] == 0x47.toByte()) { "MPEG2 TS Files that are not" }
                    i += TS_PACKET_SIZE
                }
                data = bytes
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
            return true
        }

        companion object {
            private const val TS_PACKET_SIZE = 188
            internal fun closeQuietly(closeable: Closeable?) {
                try {
                    closeable?.close()
                } catch (e: IOException) {
                    // Ignore.
                }
            }

            @Throws(
                NoSuchPaddingException::class,
                NoSuchAlgorithmException::class,
                InvalidAlgorithmParameterException::class,
                InvalidKeyException::class
            )
            internal fun getAesCp(key: ByteArray, iv: ByteArray): Cipher {
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                val skey: Key = SecretKeySpec(key, "AES")
                val param = IvParameterSpec(iv)
                cipher.init(Cipher.DECRYPT_MODE, skey, param)
                return cipher
            }
        }

    }

    class Property {
        var type: String? = null
        var properties: MutableMap<String, String?>? = null
        var values: Array<String>? = null
        var value: String? = null
        override fun toString(): String {
            val sb = StringBuilder("Property type = ")
            sb.append(type)
            if (properties != null) {
                sb.append(", properties = { ")
                properties?.let {
                    for ((key, value1) in it) {
                        sb.append(key)
                        sb.append(" = ")
                        sb.append(value1)
                        sb.append(", ")
                    }
                }
                sb.append("}")
            }
            values?.let {
                sb.append(", values = [ ")
                for (i in it) {
                    sb.append(i)
                    sb.append(", ")
                }
                sb.append("]")
            }
            return sb.toString()
        }
    }

    companion object {
        internal fun getStream(client: OkHttpClient, url: URL): InputStream? {
            val request: Request = Request.Builder()
                .url(url)
                .build()
            return try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    null
                } else response.body.byteStream()
            } catch (ignored: Exception) {
                null
            }
        }

        internal fun checkProperty(line: String): Property? {
            val property = parseLine(line)
            if (property.type == "FILE") return property
            if (property.type == "EXT-X-STREAM-INF" && property.properties != null) {
                // #EXT-X-STREAM-INF:BANDWIDTH=1280000,CODECS="...",AUDIO="aac"
                if (property.properties?.get("BANDWIDTH") != null) {
                    try {
                        property.properties?.get("BANDWIDTH")?.toLong()
                    } catch (e: NumberFormatException) {
                        return null
                    }
                }
                return property
            }
            if (property.type == "EXTINF" && property.values != null) {
                // #EXTINF:10.23,
                try {
                    property.values?.get(0)?.toDouble()
                } catch (e: NumberFormatException) {
                    return null
                }
                return property
            }
            if (property.type == "EXT-X-KEY" && property.properties != null) {
                // #EXT-X-KEY:METHOD=AES-128,URI=\"(.*)\",IV=0[xX](.{32})
                val aes = "AES-128" == property.properties?.get("METHOD")
                if (!aes && "NONE" != property.properties?.get("METHOD")) {
                    return null
                }
                if (property.properties?.containsKey("URI") != true && aes) {
                    return null
                }
                return if (property.properties?.containsKey("IV") == true && property.properties?.get(
                        "IV"
                    )?.matches(
                        Regex("0[xX](.{32})")
                    ) != true
                ) {
                    null
                } else property
            }
            return null
        }

        private fun parseLine(line: String): Property {
            val ret = Property()
            if (!line.startsWith("#")) {
                ret.type = "FILE"
                ret.value = line
                return ret
            }
            val types = line.split(Regex(":"), 2).toTypedArray()
            ret.type = types[0].substring(1)
            if (types.size == 1) {
                return ret
            }
            ret.value = types[1]
            ret.values = splitProperty(types[1])
            ret.properties = HashMap()
            ret.values?.let {
                for (p in it) {
                    if (p.isNotEmpty()) {
                        val ps = p.split(Regex("="), 2).toTypedArray()
                        if (ps.size == 1) {
                            ret.properties?.put(p, null)
                        } else {
                            val k = ps[0]
                            var v = ps[1]
                            if (v[0] == '"' && v[v.length - 1] == '"') {
                                v = v.substring(1, v.length - 1)
                            }
                            ret.properties?.put(k, v)
                        }
                    }
                }
            }
            return ret
        }

        private fun splitProperty(line: String): Array<String> {
            val list = ArrayList<String>()
            var escape = false
            var quote = false
            val sb = StringBuilder()
            for (c in line.toCharArray()) {
                if (escape) {
                    escape = false
                } else if (quote) {
                    if (c == '"') quote = false
                } else {
                    if (c == ',') {
                        list.add(sb.toString())
                        sb.delete(0, sb.length)
                        continue
                    }
                    if (c == '\\') escape = true
                    if (c == '"') quote = true
                }
                sb.append(c)
            }
            list.add(sb.toString())
            return list.toArray(arrayOf())
        }
    }
}