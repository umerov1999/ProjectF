package dev.ragnarok.fenrir.crypt

import android.util.Base64
import android.util.Log
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

object AESCrypt {
    private const val TAG = "AESCrypt"

    //AESCrypt-ObjC uses CBC and PKCS7Padding
    private const val AES_MODE = "AES/CBC/PKCS7Padding"

    //AESCrypt-ObjC uses SHA-256 (and so a 256-bit key)
    private const val HASH_ALGORITHM = "SHA-256"

    //AESCrypt-ObjC uses blank IV (not the best security, but the aim here is compatibility)
    private val ivBytes = byteArrayOf(
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00,
        0x00
    )

    //togglable log option (please turn off in live!)
    private var DEBUG_LOG_ENABLED = false

    /**
     * Generates SHA256 hash of the password which is used as key
     *
     * @param password used to generated key
     * @return SHA256 of the password
     */
    @Throws(NoSuchAlgorithmException::class)
    private fun generateKey(password: String): SecretKeySpec {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val bytes = password.toByteArray(StandardCharsets.UTF_8)
        digest.update(bytes, 0, bytes.size)
        val key = digest.digest()
        log("SHA-256 key ", key)
        return SecretKeySpec(key, "AES")
    }

    /**
     * Encrypt and encode message using 256-bit AES with key generated from password.
     *
     * @param password used to generated key
     * @param message  the thing you want to encrypt assumed String UTF-8
     * @return Base64 encoded CipherText
     * @throws GeneralSecurityException if problems occur during encryption
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(password: String, message: String): String {
        return try {
            val key = generateKey(password)
            log("message", message)
            val cipherText = encrypt(key, ivBytes, message.toByteArray(StandardCharsets.UTF_8))

            //NO_WRAP is important as was getting \n at the end
            val encoded = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            log("Base64.NO_WRAP", encoded)
            encoded
        } catch (e: UnsupportedEncodingException) {
            if (DEBUG_LOG_ENABLED) Log.e(TAG, "UnsupportedEncodingException ", e)
            throw GeneralSecurityException(e)
        }
    }

    /**
     * More flexible AES encrypt that doesn't encode
     *
     * @param key     AES key typically 128, 192 or 256 bit
     * @param iv      Initiation Vector
     * @param message in bytes (assumed it's already been decoded)
     * @return Encrypted cipher text (not encoded)
     * @throws GeneralSecurityException if something goes wrong during encryption
     */
    @Throws(GeneralSecurityException::class)
    fun encrypt(key: SecretKeySpec, iv: ByteArray, message: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val cipherText = cipher.doFinal(message)
        log("cipherText", cipherText)
        return cipherText
    }

    /**
     * Decrypt and decode ciphertext using 256-bit AES with key generated from password
     *
     * @param password                used to generated key
     * @param base64EncodedCipherText the encrpyted message encoded with base64
     * @return message in Plain text (String UTF-8)
     * @throws GeneralSecurityException if there's an issue decrypting
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(password: String, base64EncodedCipherText: String): String {
        return try {
            val key = generateKey(password)
            log("base64EncodedCipherText", base64EncodedCipherText)
            val decodedCipherText = Base64.decode(base64EncodedCipherText, Base64.NO_WRAP)
            log("decodedCipherText", decodedCipherText)
            val decryptedBytes = decrypt(key, ivBytes, decodedCipherText)
            log("decryptedBytes", decryptedBytes)
            val message = String(decryptedBytes, StandardCharsets.UTF_8)
            log("message", message)
            message
        } catch (e: UnsupportedEncodingException) {
            if (DEBUG_LOG_ENABLED) Log.e(TAG, "UnsupportedEncodingException ", e)
            throw GeneralSecurityException(e)
        }
    }

    /**
     * More flexible AES decrypt that doesn't encode
     *
     * @param key               AES key typically 128, 192 or 256 bit
     * @param iv                Initiation Vector
     * @param decodedCipherText in bytes (assumed it's already been decoded)
     * @return Decrypted message cipher text (not encoded)
     * @throws GeneralSecurityException if something goes wrong during encryption
     */
    @Throws(GeneralSecurityException::class)
    fun decrypt(key: SecretKeySpec, iv: ByteArray, decodedCipherText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val decryptedBytes = cipher.doFinal(decodedCipherText)
        log("decryptedBytes", decryptedBytes)
        return decryptedBytes
    }

    private fun log(what: String, bytes: ByteArray) {
        if (DEBUG_LOG_ENABLED) Log.d(TAG, what + "[" + bytes.size + "] [" + bytesToHex(bytes) + "]")
    }

    private fun log(what: String, value: String) {
        if (DEBUG_LOG_ENABLED) Log.d(TAG, what + "[" + value.length + "] [" + value + "]")
    }

    /**
     * Converts byte array to hexidecimal useful for logging and fault findingn
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F'
        )
        val hexChars = CharArray(bytes.size * 2)
        var v: Int
        for (j in bytes.indices) {
            v = (bytes[j] and 0xFF.toByte()).toInt()
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}