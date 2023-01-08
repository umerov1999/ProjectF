package dev.ragnarok.fenrir.crypt

import android.os.Parcel
import android.os.Parcelable
import dev.ragnarok.fenrir.kJson
import dev.ragnarok.fenrir.orZero
import dev.ragnarok.fenrir.util.ParcelUtils.readObjectInteger
import dev.ragnarok.fenrir.util.ParcelUtils.writeObjectInteger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ExchangeMessage : Parcelable {
    @SerialName("v")
    var version = 0
        private set

    @SerialName("sid")
    var sessionId: Long = 0
        private set

    @SerialName("public_key")
    var publicKey: String? = null
        private set

    @SerialName("aes_key")
    var aesKey: String? = null
        private set

    @get:SessionState
    @SessionState
    @SerialName("session_state")
    var senderSessionState = 0
        private set

    @SerialName("error_code")
    var errorCode = 0
        private set

    @KeyLocationPolicy
    @SerialName("klp")
    private var keyLocationPolicy: Int? = null

    @Suppress("UNUSED")
    constructor()
    internal constructor(builder: Builder) {
        publicKey = builder.publicKey
        version = builder.version
        sessionId = builder.sessionId
        aesKey = builder.aesKey
        senderSessionState = builder.sessionState
        errorCode = builder.errorCode
        keyLocationPolicy = builder.keyLocationPolicy
    }

    internal constructor(parcel: Parcel) {
        version = parcel.readInt()
        sessionId = parcel.readLong()
        publicKey = parcel.readString()
        aesKey = parcel.readString()
        @SessionState val s = parcel.readInt()
        senderSessionState = s
        errorCode = parcel.readInt()
        @KeyLocationPolicy val klp = readObjectInteger(parcel)
        keyLocationPolicy = klp
    }

    val isError: Boolean
        get() = errorCode != 0

    override fun toString(): String {
        return "RSA" + kJson.encodeToString(serializer(), this)
    }

    @KeyLocationPolicy
    fun getKeyLocationPolicy(): Int {
        return keyLocationPolicy.orZero()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(version)
        dest.writeLong(sessionId)
        dest.writeString(publicKey)
        dest.writeString(aesKey)
        dest.writeInt(senderSessionState)
        dest.writeInt(errorCode)
        writeObjectInteger(dest, keyLocationPolicy)
    }

    class Builder(
        val version: Int,
        val sessionId: Long,
        @SessionState val sessionState: Int
    ) {
        var publicKey: String? = null
        var aesKey: String? = null

        @KeyLocationPolicy
        var keyLocationPolicy: Int? = null
        var errorCode = 0
        fun setPublicKey(publicKey: String?): Builder {
            this.publicKey = publicKey
            return this
        }

        fun setAesKey(aesKey: String?): Builder {
            this.aesKey = aesKey
            return this
        }

        fun setErrorCode(errorCode: Int): Builder {
            this.errorCode = errorCode
            return this
        }

        fun create(): ExchangeMessage {
            return ExchangeMessage(this)
        }

        fun setKeyLocationPolicy(@KeyLocationPolicy keyLocationPolicy: Int): Builder {
            this.keyLocationPolicy = keyLocationPolicy
            return this
        }
    }

    companion object CREATOR : Parcelable.Creator<ExchangeMessage> {
        override fun createFromParcel(parcel: Parcel): ExchangeMessage {
            return ExchangeMessage(parcel)
        }

        override fun newArray(size: Int): Array<ExchangeMessage?> {
            return arrayOfNulls(size)
        }
    }
}