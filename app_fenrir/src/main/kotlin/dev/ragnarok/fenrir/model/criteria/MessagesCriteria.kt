package dev.ragnarok.fenrir.model.criteria

class MessagesCriteria(val accountId: Long, val peerId: Long) : Criteria() {
    var startMessageId: Int? = null
        private set
    var isDecryptEncryptedMessages = false
        private set

    override fun toString(): String {
        return "MessagesCriteria{" +
                "peerId=" + peerId +
                ", startMessageId=" + startMessageId +
                "} " + super.toString()
    }

    fun setStartMessageId(startMessageId: Int?): MessagesCriteria {
        this.startMessageId = startMessageId
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as MessagesCriteria
        return accountId == that.accountId && peerId == that.peerId && startMessageId == that.startMessageId
    }

    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + if (startMessageId != null) startMessageId.hashCode() else 0
        return result
    }

    fun setDecryptEncryptedMessages(decryptEncryptedMessages: Boolean): MessagesCriteria {
        isDecryptEncryptedMessages = decryptEncryptedMessages
        return this
    }
}