package dev.ragnarok.fenrir.crypt

class AesKeyPair {
    var version = 0
        private set
    var accountId = 0L
        private set
    var peerId = 0L
        private set
    var sessionId: Long = 0
        private set
    var date: Long = 0
        private set
    var startMessageId = 0
        private set
    var endMessageId = 0
        private set
    lateinit var myAesKey: String
        private set
    lateinit var hisAesKey: String
        private set

    fun setAccountId(accountId: Long): AesKeyPair {
        this.accountId = accountId
        return this
    }

    fun setPeerId(peerId: Long): AesKeyPair {
        this.peerId = peerId
        return this
    }

    fun setSessionId(sessionId: Long): AesKeyPair {
        this.sessionId = sessionId
        return this
    }

    fun setDate(date: Long): AesKeyPair {
        this.date = date
        return this
    }

    fun setStartMessageId(startMessageId: Int): AesKeyPair {
        this.startMessageId = startMessageId
        return this
    }

    fun setEndMessageId(endMessageId: Int): AesKeyPair {
        this.endMessageId = endMessageId
        return this
    }

    fun setMyAesKey(myAesKey: String?): AesKeyPair {
        if (myAesKey != null) {
            this.myAesKey = myAesKey
        }
        return this
    }

    fun setHisAesKey(hisAesKey: String?): AesKeyPair {
        if (hisAesKey != null) {
            this.hisAesKey = hisAesKey
        }
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val pair = other as AesKeyPair
        return sessionId == pair.sessionId
    }

    override fun hashCode(): Int {
        return (sessionId xor (sessionId ushr 32)).toInt()
    }

    override fun toString(): String {
        return "AesKeyPair{" +
                "version=" + version +
                ", accountId=" + accountId +
                ", peerId=" + peerId +
                ", sessionId=" + sessionId +
                ", date=" + date +
                ", startMessageId=" + startMessageId +
                ", endMessageId=" + endMessageId +
                ", myAesKey='" + myAesKey + '\'' +
                ", hisAesKey='" + hisAesKey + '\'' +
                '}'
    }

    fun setVersion(version: Int): AesKeyPair {
        this.version = version
        return this
    }
}