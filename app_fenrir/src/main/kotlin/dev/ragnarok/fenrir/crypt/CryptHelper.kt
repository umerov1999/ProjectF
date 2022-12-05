package dev.ragnarok.fenrir.crypt

import android.util.Base64
import dev.ragnarok.fenrir.crypt.AESCrypt.decrypt
import dev.ragnarok.fenrir.crypt.AESCrypt.encrypt
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.nonNullNoEmpty
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.*

object CryptHelper {

    @MessageType
    fun analizeMessageBody(text: String?): Int {
        @MessageType var type = MessageType.NORMAL
        if (text.nonNullNoEmpty()) {
            if (isKeyExchangeServiceMessage(text)) {
                type = MessageType.KEY_EXCHANGE
            } else if (isAes(text)) {
                type = MessageType.CRYPTED
            }
        }

        // Exestime.log("analizeMessageBody", start, "length: " + (text == null ? 0 : text.length()), "type: " + type);
        // 0 ms
        return type
    }

    /**
     * Является ли сообщение служебным (для обмена ключами шифрования)
     *
     * @param text текст сообщения
     * @return true - если сообщение является служебным, использовалось для обмена ключами шифрования
     */
    private fun isKeyExchangeServiceMessage(text: String): Boolean {
        return if (text.isEmpty()) {
            false
        } else try {
            if (!text.endsWith("}") || !text.startsWith("RSA{")) {
                return false
            }
            val exchangeMessageBody = text.substring(3) // without RSA on start
            val message: ExchangeMessage =
                kJson.decodeFromString(
                    ExchangeMessage.serializer(),
                    exchangeMessageBody
                )
            0 < message.sessionId && 0 < message.version && 0 < message.senderSessionState
        } catch (e: Exception) {
            false
        }
    }

    // проверяем удовлетворяет ли текст формату AES{$key_location_policy}{$session_id}:{$encrypted_body}
    // (А-аптемезацея)
    private fun isAes(text: String?): Boolean {
        if (text == null || text.isEmpty()) {
            return false
        }
        var digitsCount = 0
        var yesAes = false
        var hasDivider = false
        out@ for (i in text.indices) {
            val c = text[i]
            when (i) {
                0 -> yesAes = if ('A' == c) {
                    true
                } else {
                    break@out
                }
                1 -> if ('E' != c) {
                    yesAes = false
                }
                2 -> if ('S' != c) {
                    yesAes = false
                }
                else -> {
                    val digit = Character.isDigit(c)
                    if (digit) {
                        digitsCount++
                    } else {
                        return if (':' == c) {
                            hasDivider = true
                            break@out
                        } else {
                            false
                        }
                    }
                }
            }
            if (!yesAes) {
                break
            }
        }
        return yesAes && 1 < digitsCount && hasDivider
    }


    fun encryptWithAes(
        body: String, key: String, ifError: String, sessionId: Long,
        @KeyLocationPolicy keyLocationPolicy: Int
    ): String {
        return try {
            ("AES" + keyLocationPolicy + sessionId
                    + ":" + encrypt(key, body))
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            ifError
        }
    }


    @Throws(GeneralSecurityException::class)
    fun decryptWithAes(body: String, key: String): String {
        return decrypt(key, body)
    }


    @Throws(EncryptedMessageParseException::class)
    fun parseEncryptedMessage(body: String?): EncryptedMessage? {
        return if (body.isNullOrEmpty()) {
            null
        } else try {
            val dividerLocation = body.indexOf(':')
            @KeyLocationPolicy val keyLocationPolicy = Character.getNumericValue(body[3])
            val sessionId = body.substring(4, dividerLocation).toLong()
            val originalBody = body.substring(dividerLocation + 1)
            EncryptedMessage(sessionId, originalBody, keyLocationPolicy)
        } catch (e: Exception) {
            throw EncryptedMessageParseException()
        }

        // AES{$key_location_policy}{$session_id}:{$encrypted_body}
    }


    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun createRsaPublicKeyFromString(key: String): PublicKey {
        val byteKey = Base64.decode(key.toByteArray(), Base64.DEFAULT)
        val X509publicKey = X509EncodedKeySpec(byteKey)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(X509publicKey)
    }


    @Throws(NoSuchAlgorithmException::class)
    fun generateRandomAesKey(keysize: Int): String {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(keysize) // for example
        val secretKey = keyGen.generateKey()
        val encoded = secretKey.encoded
        return Base64.encodeToString(encoded, Base64.DEFAULT)
    }


    @Throws(NoSuchAlgorithmException::class)
    fun generateRsaKeyPair(keysize: Int): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(keysize)
        return keyGen.genKeyPair()
    }

    /**
     * Encrypt the plain text using public key.
     *
     * @param text : original plain text
     * @param key  :The public key
     * @return Encrypted text
     */

    @Throws(
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        InvalidKeyException::class,
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class
    )
    fun encryptRsa(text: String, key: PublicKey?): ByteArray {
        val cipherText: ByteArray

        // get an RSA cipher object and print the provider
        val cipher = Cipher.getInstance("RSA")
        // encrypt the plain text using the public key
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipherText = cipher.doFinal(text.toByteArray())
        return cipherText
    }

    /**
     * Decrypt text using private key.
     *
     * @param text :encrypted text
     * @param key  :The private key
     * @return plain text
     */

    @Throws(
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class
    )
    fun decryptRsa(text: ByteArray?, key: PrivateKey?): String {
        val dectyptedText: ByteArray

        // get an RSA cipher object and print the provider
        val cipher = Cipher.getInstance("RSA")

        // decrypt the text using the private key
        cipher.init(Cipher.DECRYPT_MODE, key)
        dectyptedText = cipher.doFinal(text)
        return String(dectyptedText)
    }
}