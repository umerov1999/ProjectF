package dev.ragnarok.fenrir.model

class PeerDeleting(private val accountId: Int, private val peerId: Int) {
    fun getAccountId(): Int {
        return accountId
    }

    fun getPeerId(): Int {
        return peerId
    }
}