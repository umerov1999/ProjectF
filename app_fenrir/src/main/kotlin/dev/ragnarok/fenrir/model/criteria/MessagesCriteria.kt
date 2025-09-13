package dev.ragnarok.fenrir.model.criteria

import dev.ragnarok.fenrir.orZero

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
        return other is MessagesCriteria && accountId == other.accountId && peerId == other.peerId && startMessageId == other.startMessageId
    }

    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + peerId.hashCode()
        result = 31 * result + startMessageId?.hashCode().orZero()
        return result
    }

    fun setDecryptEncryptedMessages(decryptEncryptedMessages: Boolean): MessagesCriteria {
        isDecryptEncryptedMessages = decryptEncryptedMessages
        return this
    }
}