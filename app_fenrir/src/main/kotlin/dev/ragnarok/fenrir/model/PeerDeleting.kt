package dev.ragnarok.fenrir.model

class PeerDeleting(private val accountId: Long, private val peerId: Long) {
    fun getAccountId(): Long {
        return accountId
    }

    fun getPeerId(): Long {
        return peerId
    }
}