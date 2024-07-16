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
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.isActive
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.toFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single

class MessagesDecryptor(private val store: IStorages) : IMessagesDecryptor {
    override fun withMessagesDecryption(accountId: Long): suspend (List<Message>) -> Flow<List<Message>> =
        {
            val sessions: MutableList<Pair<Int, Long>> = ArrayList(0)
            val needDecryption: MutableList<Pair<Message, EncryptedMessage>> = ArrayList(0)
            for (message in it) {
                if (message.cryptStatus != CryptStatus.ENCRYPTED) {
                    continue
                }
                try {
                    val em = parseEncryptedMessage(message.text)
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
                toFlow(it)
            } else {
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
                                val decryptedText = decryptWithAes(em.originalText, key)
                                message.decryptedText = decryptedText
                                message.cryptStatus = CryptStatus.DECRYPTED
                            } catch (e: Exception) {
                                message.cryptStatus = CryptStatus.DECRYPT_FAILED
                            }
                        }
                        it
                    }
            }
        }

    private fun getKeyPairs(
        accountId: Long,
        tokens: List<Pair<Int, Long>>
    ): Flow<LongSparseArray<AesKeyPair?>> {
        return flow {
            val keys = LongSparseArray<AesKeyPair?>(tokens.size)
            for (token in tokens) {
                if (!isActive()) {
                    break
                }
                val sessionId = token.second
                val keyPolicy = token.first
                val keyPair =
                    store.keys(keyPolicy).findKeyPairFor(accountId, sessionId).single()
                if (keyPair != null) {
                    keys.append(sessionId, keyPair)
                }
            }
            emit(keys)
        }
    }
}