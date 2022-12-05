package dev.ragnarok.fenrir.crypt

import java.security.PrivateKey
import java.util.*

class KeyExchangeSession private constructor(
    val id: Long,
    val accountId: Int,
    val peerId: Int,
    @KeyLocationPolicy val keyLocationPolicy: Int
) {

    private val messageIds: MutableSet<Int> = HashSet()

    @SessionState
    var localSessionState = 0

    @SessionState
    var oppenentSessionState = 0
    var myPrivateKey: PrivateKey? = null
    var myAesKey: String? = null
    var hisAesKey: String? = null

    fun appendMessageId(id: Int) {
        messageIds.add(id)
    }

    fun isMessageProcessed(id: Int): Boolean {
        return messageIds.contains(id)
    }

    val startMessageId: Int
        get() = Collections.min(messageIds)
    val endMessageId: Int
        get() = Collections.max(messageIds)

    companion object {
        fun createOutSession(
            id: Long,
            accountId: Int,
            peerId: Int,
            @KeyLocationPolicy keyLocationPolicy: Int
        ): KeyExchangeSession {
            val session = KeyExchangeSession(id, accountId, peerId, keyLocationPolicy)
            session.localSessionState = SessionState.INITIATOR_EMPTY
            return session
        }

        fun createInputSession(
            id: Long,
            accountId: Int,
            peerId: Int,
            @KeyLocationPolicy keyLocationPolicy: Int
        ): KeyExchangeSession {
            val session = KeyExchangeSession(id, accountId, peerId, keyLocationPolicy)
            session.localSessionState = SessionState.NO_INITIATOR_EMPTY
            return session
        }
    }
}