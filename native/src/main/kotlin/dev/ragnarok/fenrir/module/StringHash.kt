package dev.ragnarok.fenrir.module

import java.security.MessageDigest

object StringHash {
    private external fun getSha1(value: String): String
    private external fun getSha1ByteArray(value: ByteArray): String
    private external fun getCRC32(value: String): Int

    fun calculateSha1(value: String): String {
        return if (FenrirNative.isNativeLoaded) {
            getSha1(value)
        } else try {
            val crypt = MessageDigest.getInstance("SHA-1")
            crypt.reset()
            crypt.update(value.toByteArray(Charsets.UTF_8))
            crypt.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            "error"
        }
    }

    fun calculateSha1(value: ByteArray): String {
        return if (FenrirNative.isNativeLoaded) {
            getSha1ByteArray(value)
        } else try {
            val crypt = MessageDigest.getInstance("SHA-1")
            crypt.reset()
            crypt.update(value)
            crypt.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            "error"
        }
    }

    fun calculateCRC32(value: String): Int {
        return if (FenrirNative.isNativeLoaded) {
            getCRC32(value)
        } else value.hashCode()
    }
}