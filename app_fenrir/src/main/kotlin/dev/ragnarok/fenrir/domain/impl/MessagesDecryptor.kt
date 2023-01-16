package dev.ragnarok.fenrir.domain.impl

import android.util.LongSparseArray
import dev.ragnarok.fenrir.crypt.AesKeyPair
import dev.ragnarok.fenrir.crypt.CryptHelper.decryptWithAes
import dev.ragnarok.fenrir.crypt.CryptHelper.parseEncryptedMessage
import dev.ragnarok.fenrir.crypt.EncryptedMessage
import dev.ragnarok.fenrir.db.interfaces.IStorages
import dev.ragnarok.fenrir.domain.IMessagesDecryptor
import dev.ragnarok.fenrir.model.CryptStatus
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.core.SingleTransformer

class MessagesDecryptor(private val store: IStorages) : IMessagesDecryptor {
    override fun withMessagesDecryption(accountId: Long): SingleTransformer<List<Message>, List<Message>> {
        return SingleTransformer { single: Single<List<Message>> ->
            single
                .flatMap { messages ->
                    val sessions: MutableList<Pair<Int, Long>> = ArrayList(0)
                    val needDecryption: MutableList<Pair<Message, EncryptedMessage>> = ArrayList(0)
                    for (message in messages) {
                        if (message.cryptStatus != CryptStatus.ENCRYPTED) {
                            continue
                        }
                        try {
                            val em = parseEncryptedMessage(message.body)
                            if (em != null) {
                                needDecryption.add(create(message, em))
                                sessions.add(create(em.KeyLocationPolicy, em.sessionId))
                            } else {
                                message.cryptStatus = CryptStatus.DECRYPT_FAILED
                            }
                        } catch (e: Exception) {
                            message.cryptStatus = CryptStatus.DECRYPT_FAILED
                        }
                    }
                    if (needDecryption.isEmpty()) {
                        return@flatMap Single.just(messages)
                    }
                    getKeyPairs(accountId, sessions)
                        .map { keys ->
                            for (pair in needDecryption) {
                                val message = pair.first
                                val em = pair.second
                                try {
                                    val keyPair = keys[em.sessionId]
                                    if (keyPair == null) {
                                        message.cryptStatus = CryptStatus.DECRYPT_FAILED
                                        continue
                                    }
                                    val key =
                                        if (message.isOut) keyPair.myAesKey else keyPair.hisAesKey
                                    val decryptedBody = decryptWithAes(em.originalBody, key)
                                    message.decryptedBody = decryptedBody
                                    message.cryptStatus = CryptStatus.DECRYPTED
                                } catch (e: Exception) {
                                    message.cryptStatus = CryptStatus.DECRYPT_FAILED
                                }
                            }
                            messages
                        }
                }
        }
    }

    private fun getKeyPairs(
        accountId: Long,
        tokens: List<Pair<Int, Long>>
    ): Single<LongSparseArray<AesKeyPair?>> {
        return Single.create { emitter: SingleEmitter<LongSparseArray<AesKeyPair?>> ->
            val keys = LongSparseArray<AesKeyPair?>(tokens.size)
            for (token in tokens) {
                if (emitter.isDisposed) {
                    break
                }
                val sessionId = token.second
                val keyPolicy = token.first
                val keyPair =
                    store.keys(keyPolicy).findKeyPairFor(accountId, sessionId).blockingGet()
                if (keyPair != null) {
                    keys.append(sessionId, keyPair)
                }
            }
            emitter.onSuccess(keys)
        }
    }
}